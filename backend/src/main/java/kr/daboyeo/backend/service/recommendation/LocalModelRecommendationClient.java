package kr.daboyeo.backend.service.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.daboyeo.backend.config.RecommendationProperties;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiPick;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiResult;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ScoredCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LocalModelRecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(LocalModelRecommendationClient.class);
    private static final TypeReference<Map<String, List<AiPick>>> AI_RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int FAST_READ_TIMEOUT_MS = 45_000;
    private static final int PRECISE_READ_TIMEOUT_MS = 90_000;

    private final RecommendationProperties properties;
    private final ObjectMapper objectMapper;

    public LocalModelRecommendationClient(RecommendationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<AiResult> rankAndExplain(
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        RestClient restClient = RestClient.builder()
            .baseUrl(properties.lmStudioBaseUrl())
            .requestFactory(requestFactory(mode))
            .build();
        String model = properties.modelFor(mode);
        if (model.isBlank()) {
            return Optional.empty();
        }

        try {
            return callLmStudio(restClient, mode, profile, candidates, model);
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Local recommendation model call failed. mode={}, model={}, cause={}", mode.wireValue(), model, e.toString());
            return Optional.empty();
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(RecommendationMode mode) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(mode == RecommendationMode.FAST ? FAST_READ_TIMEOUT_MS : PRECISE_READ_TIMEOUT_MS);
        return factory;
    }

    private Optional<AiResult> callLmStudio(
        RestClient restClient,
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates,
        String model
    ) throws JsonProcessingException {
        Map<String, Object> request = Map.of(
            "model", model,
            "stream", false,
            "temperature", mode == RecommendationMode.FAST ? 0.1 : 0.2,
            "max_tokens", 260,
            "messages", messages(mode, profile, candidates),
            "response_format", recommendationResponseFormat()
        );
        JsonNode response = restClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(JsonNode.class);
        String content = response == null
            ? ""
            : response.path("choices").path(0).path("message").path("content").asText("");
        return parseResult(content, model);
    }

    private List<Map<String, String>> messages(
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates
    ) throws JsonProcessingException {
        return List.of(
            Map.of(
                "role", "system",
                "content", "You are DABOYEO's movie recommendation assistant. Never invent candidates or showtimes."
            ),
            Map.of("role", "user", "content", buildPrompt(mode, profile, candidates))
        );
    }

    private Optional<AiResult> parseResult(String content, String model) throws JsonProcessingException {
        String json = extractJson(content);
        if (json.isBlank()) {
            return Optional.empty();
        }
        Map<String, List<AiPick>> parsed = objectMapper.readValue(json, AI_RESPONSE_TYPE);
        return Optional.of(new AiResult(json, model, parsed.getOrDefault("recommendations", List.of())));
    }

    private String buildPrompt(RecommendationMode mode, TagProfile profile, List<ScoredCandidate> candidates) {
        try {
            return """
                User conditions:
                - audience: %s
                - mood: %s
                - avoid: %s
                - mode: %s

                Candidate JSON:
                %s

                Task:
                - Choose up to 3 candidates.
                - Use only showtimeId values that exist in the candidate JSON.
                - Write reason, caution, and valuePoint in Korean.
                - Keep each reason, caution, and valuePoint as one short sentence.
                - For fast mode, keep wording very short.
                - For precise mode, mention the reflected user condition more clearly.
                """.formatted(
                profile.audience(),
                profile.mood(),
                profile.avoid(),
                mode.wireValue(),
                objectMapper.writeValueAsString(candidates.stream().map(this::candidateForPrompt).toList())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build recommendation prompt.", e);
        }
    }

    private Map<String, Object> recommendationResponseFormat() {
        return Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                "name", "daboyeo_recommendation_response",
                "strict", true,
                "schema", recommendationResponseSchema()
            )
        );
    }

    private Map<String, Object> recommendationResponseSchema() {
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("recommendations"),
            "properties", Map.of(
                "recommendations", Map.of(
                    "type", "array",
                    "maxItems", 3,
                    "items", recommendationItemSchema()
                )
            )
        );
    }

    private Map<String, Object> recommendationItemSchema() {
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("showtimeId", "reason", "caution", "valuePoint"),
            "properties", Map.of(
                "showtimeId", Map.of("type", "integer"),
                "reason", Map.of("type", "string", "maxLength", 80),
                "caution", Map.of("type", "string", "maxLength", 80),
                "valuePoint", Map.of("type", "string", "maxLength", 80)
            )
        );
    }

    private Map<String, Object> candidateForPrompt(ScoredCandidate scored) {
        var candidate = scored.candidate();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("showtimeId", candidate.showtimeId());
        value.put("title", candidate.title());
        value.put("score", scored.score());
        value.put("tags", candidate.allTags());
        value.put("penalties", scored.penalties());
        value.put("runtimeMinutes", candidate.runtimeMinutes());
        value.put("ageRating", candidate.ageRating());
        value.put("theaterName", candidate.theaterName());
        value.put("regionName", candidate.regionName());
        value.put("startsAt", candidate.startsAt());
        value.put("minPriceAmount", candidate.minPriceAmount());
        value.put("screenType", candidate.screenType());
        return value;
    }

    private String extractJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        }
        return trimmed;
    }
}

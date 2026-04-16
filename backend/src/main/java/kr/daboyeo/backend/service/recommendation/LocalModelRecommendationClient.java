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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LocalModelRecommendationClient {

    private static final TypeReference<Map<String, List<AiPick>>> AI_RESPONSE_TYPE = new TypeReference<>() {
    };

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
            .baseUrl(properties.ollamaBaseUrl())
            .build();
        String model = properties.modelFor(mode);
        if (model.isBlank()) {
            return Optional.empty();
        }

        try {
            return callOllama(restClient, mode, profile, candidates, model);
        } catch (RestClientException | JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private Optional<AiResult> callOllama(
        RestClient restClient,
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates,
        String model
    ) throws JsonProcessingException {
        Map<String, Object> request = Map.of(
            "model", model,
            "stream", false,
            "think", false,
            "format", "json",
            "messages", messages(mode, profile, candidates),
            "options", Map.of(
                "temperature", mode == RecommendationMode.FAST ? 0.1 : 0.2,
                "num_predict", mode == RecommendationMode.FAST ? 220 : 420
            )
        );
        JsonNode response = restClient.post()
            .uri("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(JsonNode.class);
        String content = response == null ? "" : response.path("message").path("content").asText("");
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
                "content", "너는 DABOYEO 영화 추천 보조 엔진이다. 후보 밖 영화는 절대 추천하지 말고 JSON만 반환한다."
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
                사용자 조건:
                - audience: %s
                - mood: %s
                - avoid: %s
                - mode: %s

                후보 JSON:
                %s

                작업:
                - 후보 중 최대 3개만 골라라.
                - showtimeId는 후보에 있는 값만 써라.
                - reason, caution, valuePoint는 각각 한국어 한 문장으로 짧게 써라.
                - 빠른 추천이면 더 짧게, 정밀 추천이면 조건 반영 이유를 조금 더 분명히 써라.
                - JSON 형식만 반환해라.

                반환 형식:
                {
                  "recommendations": [
                    {
                      "showtimeId": 123,
                      "reason": "친구와 보기 좋고 빠른 전개 취향에 맞아요.",
                      "caution": "잔인한 장면이 부담되면 예고편을 먼저 확인해요.",
                      "valuePoint": "저녁 시간대와 가격 조건이 좋아요."
                    }
                  ]
                }
                """.formatted(
                profile.audience(),
                profile.mood(),
                profile.avoid(),
                mode.wireValue(),
                objectMapper.writeValueAsString(candidates.stream().map(this::candidateForPrompt).toList())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("추천 프롬프트를 만들지 못했어.", e);
        }
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

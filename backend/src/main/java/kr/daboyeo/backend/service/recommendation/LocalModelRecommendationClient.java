package kr.daboyeo.backend.service.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import kr.daboyeo.backend.config.RecommendationProperties;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiPick;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiResult;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ScoredCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LocalModelRecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(LocalModelRecommendationClient.class);
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
            "temperature", mode == RecommendationMode.FAST ? 0.0 : 0.05,
            "top_p", 0.85,
            "max_tokens", properties.maxTokensFor(mode),
            "messages", messages(mode, profile, candidates),
            "response_format", recommendationResponseFormat(mode, properties.responseTextMaxLength(), candidates.size())
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
                "content", mode == RecommendationMode.PRECISE
                    ? "Rerank only given showtimes and write one Korean hashtag analysis. Return JSON only. No prose, invention, scores, or raw tokens."
                    : "Rank only given showtimes. Return JSON only. why/v are Korean hashtags from b/vp. No prose, invention, scores, or raw tokens."
            ),
            Map.of("role", "user", "content", buildPrompt(mode, profile, candidates))
        );
    }

    Optional<AiResult> parseResult(String content, String model) throws JsonProcessingException {
        String json = extractJson(content);
        if (json.isBlank()) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(json);
        JsonNode rows = root.path("r");
        List<AiPick> picks = new ArrayList<>();
        if (rows.isArray()) {
            rows.forEach(row -> {
                if (row.isIntegralNumber()) {
                    picks.add(new AiPick(row.asLong(), "", "", ""));
                } else if (row.isObject()) {
                    picks.add(new AiPick(
                        row.path("id").asLong(),
                        row.path("why").asText(""),
                        "",
                        row.path("v").asText(""),
                        row.path("a").asText("")
                    ));
                }
            });
        }
        return Optional.of(new AiResult(json, model, picks));
    }

    String buildPrompt(RecommendationMode mode, TagProfile profile, List<ScoredCandidate> candidates) {
        try {
            String candidateJson = objectMapper.writeValueAsString(candidates.stream().map(this::candidateForPrompt).toList());
            if (mode == RecommendationMode.PRECISE) {
                String pickInstruction = candidates.size() >= 3
                    ? "Pick exactly 3 objects from JSON."
                    : "Pick all " + candidates.size() + " objects from JSON.";
                return """
                    C aud=%s mood=%s avoid=%s liked=%s
                    %s

                    Return only {"r":[{"id":1,"a":"#SF취향"}]}.
                    %s Use b first, vp as tie-break.
                    a=copy one hashtag from liked when liked is not empty; otherwise one short hashtag from b.
                    No underscores, combined tags, title, prose, field names, raw tokens, or score. Max %d chars.
                    """.formatted(
                    profile.audience(),
                    profile.mood(),
                    profile.avoid(),
                    analysisHints(profile),
                    candidateJson,
                    pickInstruction,
                    properties.responseTextMaxLength()
                );
            }
            return """
                C aud=%s mood=%s avoid=%s mode=%s
                %s

                Return only {"r":[{"id":1,"why":"#가볍게 #친구랑","v":"#17:00상영 #좌석여유"}]}.
                Pick max 3 ids from JSON. why=b tags only. v=vp tags only.
                Korean hashtags only, max %d chars each. No title, prose, field names, raw tokens, or score.
                """.formatted(
                profile.audience(),
                profile.mood(),
                profile.avoid(),
                mode.wireValue(),
                candidateJson,
                properties.responseTextMaxLength()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build recommendation prompt.", e);
        }
    }

    private Map<String, Object> recommendationResponseFormat(RecommendationMode mode, int maxTextLength, int candidateCount) {
        return Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                "name", "daboyeo_recommendation_response",
                "strict", true,
                "schema", recommendationResponseSchema(mode, maxTextLength, candidateCount)
            )
        );
    }

    private Map<String, Object> recommendationResponseSchema(RecommendationMode mode, int maxTextLength, int candidateCount) {
        int minItems = mode == RecommendationMode.PRECISE ? Math.min(3, Math.max(1, candidateCount)) : 0;
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("r"),
            "properties", Map.of(
                "r", Map.of(
                    "type", "array",
                    "minItems", minItems,
                    "maxItems", 3,
                    "items", mode == RecommendationMode.PRECISE
                        ? preciseRecommendationItemSchema(maxTextLength)
                        : recommendationItemSchema(maxTextLength)
                )
            )
        );
    }

    private Map<String, Object> preciseRecommendationItemSchema(int maxTextLength) {
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("id", "a"),
            "properties", Map.of(
                "id", Map.of("type", "integer"),
                "a", Map.of("type", "string", "maxLength", Math.min(maxTextLength, 18))
            )
        );
    }

    private Map<String, Object> recommendationItemSchema(int maxTextLength) {
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("id", "why", "v"),
            "properties", Map.of(
                "id", Map.of("type", "integer"),
                "why", Map.of("type", "string", "maxLength", maxTextLength),
                "v", Map.of("type", "string", "maxLength", maxTextLength)
            )
        );
    }

    private Map<String, Object> candidateForPrompt(ScoredCandidate scored) {
        var candidate = scored.candidate();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", candidate.showtimeId());
        value.put("t", candidate.title());
        value.put("b", reasonHints(scored));
        value.put("vp", valueHints(candidate));
        return value;
    }

    private List<String> analysisHints(TagProfile profile) {
        if (profile == null || profile.likedGenres().isEmpty()) {
            return List.of();
        }
        return profile.likedGenres().stream()
            .map(this::genreAnalysisHint)
            .filter(value -> !value.isBlank())
            .distinct()
            .limit(4)
            .toList();
    }

    private String genreAnalysisHint(String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.startsWith("genre:")) {
            normalized = normalized.substring("genre:".length());
        }
        String label = switch (normalized) {
            case "action" -> "액션";
            case "adventure" -> "어드벤처";
            case "animation" -> "애니메이션";
            case "comedy" -> "코미디";
            case "crime" -> "범죄";
            case "drama" -> "드라마";
            case "family" -> "가족";
            case "fantasy" -> "판타지";
            case "history" -> "역사";
            case "horror" -> "공포";
            case "music" -> "음악";
            case "musical" -> "뮤지컬";
            case "mystery" -> "미스터리";
            case "romance" -> "로맨스";
            case "sf", "sci-fi", "science-fiction" -> "SF";
            case "thriller" -> "스릴러";
            default -> normalized;
        };
        return label.isBlank() ? "" : "#" + label + "취향";
    }

    private List<String> reasonHints(ScoredCandidate scored) {
        var candidate = scored.candidate();
        List<String> hints = new ArrayList<>();
        scored.matchedTags().stream()
            .filter(this::isReasonSourceTag)
            .map(this::tagPhrase)
            .filter(value -> !value.isBlank())
            .forEach(hints::add);
        if (hints.isEmpty()) {
            candidate.allTags().stream()
                .filter(this::isReasonSourceTag)
                .map(this::tagPhrase)
                .filter(value -> !value.isBlank())
                .limit(3)
                .forEach(hints::add);
        }
        if (candidate.runtimeMinutes() != null && candidate.runtimeMinutes() <= 125) {
            hints.add("#부담적은러닝타임");
        }
        if (!candidate.ageRating().isBlank()) {
            hints.add(ageTag(candidate.ageRating()));
        }
        if (hints.isEmpty()) {
            hints.add("#조건근접");
        }
        return hints.stream().distinct().limit(5).toList();
    }

    private boolean isReasonSourceTag(String tag) {
        return tag != null && !tag.trim().toLowerCase(Locale.ROOT).startsWith("content:");
    }

    private List<String> valueHints(kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate candidate) {
        List<String> hints = new ArrayList<>();
        if (candidate.startsAt() != null) {
            hints.add("#" + DateTimeFormatter.ofPattern("HH:mm").format(candidate.startsAt()) + "상영");
        }
        if (candidate.minPriceAmount() != null) {
            hints.add("#" + candidate.minPriceAmount() + "원");
        }
        String seat = seatHint(candidate.remainingSeatCount(), candidate.totalSeatCount());
        if ("enough".equals(seat)) {
            hints.add("#좌석여유");
        } else if ("limited".equals(seat)) {
            hints.add("#좌석주의");
        }
        if (!candidate.theaterName().isBlank()) {
            hints.add("#예매가능");
        }
        if (hints.isEmpty()) {
            hints.add("#예매정보");
        }
        return hints.stream().distinct().limit(4).toList();
    }

    private String tagPhrase(String tag) {
        if (tag == null || tag.isBlank()) {
            return "";
        }
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "audience:alone" -> "#혼영";
            case "audience:friends" -> "#친구랑";
            case "audience:date" -> "#데이트";
            case "audience:family" -> "#가족";
            case "audience:child" -> "#아이와함께";
            case "mood:light" -> "#가볍게";
            case "mood:immersive" -> "#몰입";
            case "mood:exciting" -> "#신나는";
            case "mood:calm" -> "#잔잔한";
            case "mood:tense" -> "#긴장감";
            case "mood:warm" -> "#따뜻한";
            case "mood:visual" -> "#시각적재미";
            case "pace:easy" -> "#이해쉬움";
            case "pace:fast" -> "#빠른전개";
            case "pace:slow" -> "#천천히몰입";
            case "content:too_long" -> "#긴영화주의";
            case "content:complex" -> "#난도주의";
            case "content:violence" -> "#잔인함주의";
            case "content:sad_ending" -> "#슬픈결말주의";
            case "content:loud" -> "#큰소리주의";
            default -> {
                if (normalized.startsWith("genre:")) {
                    yield "#" + normalized.substring("genre:".length()).replace('_', '-');
                }
                yield "";
            }
        };
    }

    private String ageTag(String ageRating) {
        String age = ageRating == null ? "" : ageRating.trim();
        if (age.isBlank()) {
            return "";
        }
        String compact = age
            .replace("이상", "")
            .replace("관람가", "")
            .replace("관람", "")
            .replaceAll("\\s+", "");
        if (compact.matches("\\d+")) {
            compact = compact + "세";
        }
        return "#" + compact;
    }

    private String seatHint(Integer remainingSeatCount, Integer totalSeatCount) {
        if (remainingSeatCount == null) {
            return "";
        }
        if (remainingSeatCount <= 0) {
            return "none";
        }
        if (totalSeatCount == null || totalSeatCount <= 0) {
            return remainingSeatCount >= 20 ? "enough" : "limited";
        }
        double ratio = remainingSeatCount / (double) totalSeatCount;
        if (remainingSeatCount >= 30 || ratio >= 0.3) {
            return "enough";
        }
        if (remainingSeatCount <= 10 || ratio <= 0.1) {
            return "limited";
        }
        return "normal";
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

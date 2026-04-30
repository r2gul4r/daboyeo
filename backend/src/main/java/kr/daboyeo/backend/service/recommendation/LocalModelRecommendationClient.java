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
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiProvider;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiProviderStatus;
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
    private static final int STATUS_TIMEOUT_MS = 1_500;
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
        return rankAndExplain(AiProvider.LOCAL, mode, profile, candidates);
    }

    public Optional<AiResult> rankAndExplain(
        AiProvider provider,
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        RestClient restClient = RestClient.builder()
            .baseUrl(properties.baseUrlFor(provider))
            .requestFactory(requestFactory(mode))
            .build();
        String model = properties.modelFor(provider, mode);
        if (model.isBlank()) {
            return Optional.empty();
        }

        try {
            return callOpenAiCompatible(restClient, provider, mode, profile, candidates, model);
        } catch (RestClientException | JsonProcessingException e) {
            log.warn(
                "Recommendation model call failed. provider={}, mode={}, model={}, cause={}",
                provider.wireValue(),
                mode.wireValue(),
                model,
                e.toString()
            );
            return Optional.empty();
        }
    }

    public AiProviderStatus providerStatus(AiProvider provider) {
        List<String> expectedModels = expectedModels(provider);
        if (properties.baseUrlFor(provider).isBlank() || expectedModels.isEmpty()) {
            return new AiProviderStatus(
                provider.wireValue(),
                properties.providerLabel(provider),
                expectedModels,
                false,
                "not_configured",
                "모델 라우팅 설정이 비어 있어."
            );
        }

        try {
            JsonNode response = RestClient.builder()
                .baseUrl(properties.baseUrlFor(provider))
                .requestFactory(statusRequestFactory())
                .build()
                .get()
                .uri("/models")
                .retrieve()
                .body(JsonNode.class);
            List<String> actualModels = modelIds(response);
            List<String> missingModels = expectedModels.stream()
                .filter(model -> !actualModels.contains(model))
                .toList();
            if (!missingModels.isEmpty()) {
                return new AiProviderStatus(
                    provider.wireValue(),
                    properties.providerLabel(provider),
                    expectedModels,
                    false,
                    "model_missing",
                    "서버는 켜져 있지만 필요한 모델이 로드되지 않았어."
                );
            }
            return new AiProviderStatus(
                provider.wireValue(),
                properties.providerLabel(provider),
                expectedModels,
                true,
                "ready",
                "모델 서버가 응답 중이야."
            );
        } catch (RestClientException exception) {
            return new AiProviderStatus(
                provider.wireValue(),
                properties.providerLabel(provider),
                expectedModels,
                false,
                "offline",
                "모델 서버에 연결할 수 없어. 결과는 코드 점수 기반 fallback으로 나올 수 있어."
            );
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(RecommendationMode mode) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(mode == RecommendationMode.FAST ? FAST_READ_TIMEOUT_MS : PRECISE_READ_TIMEOUT_MS);
        return factory;
    }

    private SimpleClientHttpRequestFactory statusRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(STATUS_TIMEOUT_MS);
        factory.setReadTimeout(STATUS_TIMEOUT_MS);
        return factory;
    }

    private List<String> expectedModels(AiProvider provider) {
        if (provider == AiProvider.GPT) {
            return List.of(properties.modelFor(provider, RecommendationMode.FAST)).stream()
                .filter(model -> model != null && !model.isBlank())
                .distinct()
                .toList();
        }
        return List.of(
                properties.modelFor(AiProvider.LOCAL, RecommendationMode.FAST),
                properties.modelFor(AiProvider.LOCAL, RecommendationMode.PRECISE)
            ).stream()
            .filter(model -> model != null && !model.isBlank())
            .distinct()
            .toList();
    }

    private List<String> modelIds(JsonNode response) {
        List<String> ids = new ArrayList<>();
        JsonNode data = response == null ? null : response.path("data");
        if (data != null && data.isArray()) {
            for (JsonNode item : data) {
                String id = item.path("id").asText("");
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Optional<AiResult> callOpenAiCompatible(
        RestClient restClient,
        AiProvider provider,
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates,
        String model
    ) throws JsonProcessingException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("stream", false);
        request.put("temperature", mode == RecommendationMode.FAST ? 0.0 : 0.05);
        request.put("top_p", 0.85);
        request.put("max_tokens", properties.maxTokensFor(provider, mode));
        request.put("messages", messages(provider, mode, profile, candidates));
        request.put(
            "response_format",
            recommendationResponseFormat(
                provider,
                mode,
                properties.responseTextMaxLengthFor(provider, mode),
                candidates.size()
            )
        );

        String reasoningEffort = properties.reasoningEffortFor(provider, mode);
        if (!reasoningEffort.isBlank()) {
            request.put("reasoning_effort", reasoningEffort);
        }

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
        AiProvider provider,
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates
    ) throws JsonProcessingException {
        return List.of(
            Map.of(
                "role", "system",
                "content", systemPrompt(provider, mode)
            ),
            Map.of("role", "user", "content", buildPrompt(provider, mode, profile, candidates))
        );
    }

    private String systemPrompt(AiProvider provider, RecommendationMode mode) {
        if (provider == AiProvider.GPT) {
            return mode == RecommendationMode.PRECISE
                ? "You are a careful Korean movie recommendation analyst. Use only the supplied candidates. Do deep comparative reasoning across user context, avoided elements, poster taste, practical showtime value, and tradeoffs between close candidates. Return JSON only. Do not invent movies, theaters, prices, scores, ids, seats, runtimes, or hidden fields."
                : "You are a concise Korean movie recommendation analyst. Use only the supplied candidates. Be faster than deep analysis but still compare the supplied evidence more intelligently than a local tag ranker. Return JSON only. Do not invent movies, theaters, prices, scores, ids, seats, runtimes, or hidden fields.";
        }
        return mode == RecommendationMode.PRECISE
            ? "Rerank only given showtimes and write one Korean hashtag analysis. Return JSON only. No prose, invention, scores, or raw tokens."
            : "Rank only given showtimes. Return JSON only. why/v are Korean hashtags from b/vp. No prose, invention, scores, or raw tokens.";
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
                        row.path("c").asText(""),
                        row.path("v").asText(""),
                        row.path("a").asText("")
                    ));
                }
            });
        }
        return Optional.of(new AiResult(json, model, picks));
    }

    String buildPrompt(RecommendationMode mode, TagProfile profile, List<ScoredCandidate> candidates) {
        return buildPrompt(AiProvider.LOCAL, mode, profile, candidates);
    }

    String buildPrompt(
        AiProvider provider,
        RecommendationMode mode,
        TagProfile profile,
        List<ScoredCandidate> candidates
    ) {
        TagProfile safeProfile = profile == null ? new TagProfile() : profile;
        try {
            String candidateJson = objectMapper.writeValueAsString(candidates.stream()
                .map(scored -> provider == AiProvider.GPT ? candidateForGptPrompt(scored, safeProfile, mode) : candidateForPrompt(scored))
                .toList());
            if (provider == AiProvider.GPT) {
                String pickInstruction = mode == RecommendationMode.PRECISE && candidates.size() >= 3
                    ? "Pick exactly 3 objects from candidates."
                    : "Pick 1 to " + Math.min(3, candidates.size()) + " objects from candidates.";
                String depth = mode == RecommendationMode.PRECISE
                    ? "Decision style: GPT_PRECISE. Evaluate every supplied candidate before choosing. Compare user intent, poster taste, avoid risks, showtime practicality, and why each selected candidate beats a nearby alternative."
                    : "Decision style: GPT_FAST. Make a single-pass but evidence-based comparison across the supplied candidates. Prioritize the strongest practical fit and do not write exhaustive tradeoffs.";
                String itemContract = mode == RecommendationMode.PRECISE
                    ? "why=2 short Korean sentences naming the decisive fit; a=2-3 Korean sentences covering poster taste, avoid-risk handling, and tradeoff versus another candidate; v=one Korean sentence about practical showtime/theater value; c=short Korean caution or empty string."
                    : "why=1 Korean sentence naming the decisive fit; a=one Korean sentence about taste or context; v=one Korean sentence about practical showtime/theater value; c=short Korean caution or empty string.";
                String comparisonFields = mode == RecommendationMode.PRECISE
                    ? "- watchRisks/tradeoffHints: reasons to be careful or compare against nearby options"
                    : "- watchRisks: reasons to be careful";
                return """
                    User profile:
                    - audience=%s
                    - mood=%s
                    - avoid=%s
                    - liked_poster_hints=%s

                    Candidates:
                    %s

                    Use these candidate fields:
                    - tasteMatch: poster and genre evidence for this user
                    - fitHints: direct fit signals
                    - scheduleFit/practicalValue: booking-time and theater practicality
                    %s

                    Method:
                    %s
                    %s
                    Return only {"r":[{"id":1,"why":"...","a":"...","v":"...","c":"..."}]}.
                    %s
                    Do not mention scores, raw tags, JSON field names, or unavailable facts.
                    Never invent movies, theaters, prices, seats, runtimes, showtimes, or booking availability.
                    """.formatted(
                    safeProfile.audience(),
                    safeProfile.mood(),
                    safeProfile.avoid(),
                    analysisHints(safeProfile),
                    candidateJson,
                    comparisonFields,
                    depth,
                    pickInstruction,
                    itemContract
                );
            }
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
                    safeProfile.audience(),
                    safeProfile.mood(),
                    safeProfile.avoid(),
                    analysisHints(safeProfile),
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
                safeProfile.audience(),
                safeProfile.mood(),
                safeProfile.avoid(),
                mode.wireValue(),
                candidateJson,
                properties.responseTextMaxLength()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build recommendation prompt.", e);
        }
    }

    private Map<String, Object> recommendationResponseFormat(
        AiProvider provider,
        RecommendationMode mode,
        int maxTextLength,
        int candidateCount
    ) {
        return Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                "name", "daboyeo_recommendation_response",
                "strict", true,
                "schema", recommendationResponseSchema(provider, mode, maxTextLength, candidateCount)
            )
        );
    }

    private Map<String, Object> recommendationResponseSchema(
        AiProvider provider,
        RecommendationMode mode,
        int maxTextLength,
        int candidateCount
    ) {
        int minItems = provider == AiProvider.GPT
            ? mode == RecommendationMode.PRECISE ? Math.min(3, Math.max(1, candidateCount)) : 1
            : mode == RecommendationMode.PRECISE ? Math.min(3, Math.max(1, candidateCount)) : 0;
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("r"),
            "properties", Map.of(
                "r", Map.of(
                    "type", "array",
                    "minItems", minItems,
                    "maxItems", 3,
                    "items", provider == AiProvider.GPT
                        ? gptRecommendationItemSchema(mode, maxTextLength)
                        : mode == RecommendationMode.PRECISE
                        ? preciseRecommendationItemSchema(maxTextLength)
                        : recommendationItemSchema(maxTextLength)
                )
            )
        );
    }

    private Map<String, Object> gptRecommendationItemSchema(RecommendationMode mode, int maxTextLength) {
        int analysisMax = mode == RecommendationMode.PRECISE ? maxTextLength : Math.min(maxTextLength, 150);
        int reasonMax = mode == RecommendationMode.PRECISE ? Math.min(maxTextLength, 220) : Math.min(maxTextLength, 140);
        int valueMax = mode == RecommendationMode.PRECISE ? Math.min(maxTextLength, 170) : Math.min(maxTextLength, 130);
        int cautionMax = mode == RecommendationMode.PRECISE ? Math.min(maxTextLength, 140) : Math.min(maxTextLength, 100);
        return Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("id", "why", "a", "v", "c"),
            "properties", Map.of(
                "id", Map.of("type", "integer"),
                "why", Map.of("type", "string", "maxLength", reasonMax),
                "a", Map.of("type", "string", "maxLength", analysisMax),
                "v", Map.of("type", "string", "maxLength", valueMax),
                "c", Map.of("type", "string", "maxLength", cautionMax)
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

    private Map<String, Object> candidateForGptPrompt(ScoredCandidate scored, TagProfile profile, RecommendationMode mode) {
        var candidate = scored.candidate();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", candidate.showtimeId());
        value.put("title", candidate.title());
        value.put("theater", List.of(candidate.providerCode(), candidate.theaterName(), candidate.regionName(), candidate.screenName())
            .stream()
            .filter(part -> part != null && !part.isBlank())
            .toList());
        value.put("startsAt", candidate.startsAt() == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(candidate.startsAt()));
        value.put("price", priceSummary(candidate.minPriceAmount(), candidate.currencyCode()));
        value.put("seats", seatSummary(candidate.remainingSeatCount(), candidate.totalSeatCount()));
        value.put("age", candidate.ageRating());
        value.put("runtimeMinutes", candidate.runtimeMinutes());
        value.put("tasteMatch", tasteMatchHints(scored, profile));
        value.put("fitHints", reasonHints(scored));
        value.put("scheduleFit", scheduleFit(candidate));
        value.put("practicalValue", valueHints(candidate));
        value.put("watchRisks", cautionHints(scored));
        if (mode == RecommendationMode.PRECISE) {
            value.put("tradeoffHints", tradeoffHints(scored, profile));
        }
        return value;
    }

    private String priceSummary(Integer amount, String currencyCode) {
        if (amount == null) {
            return "";
        }
        String currency = currencyCode == null || currencyCode.isBlank() ? "KRW" : currencyCode.trim();
        return amount + " " + currency;
    }

    private List<String> tasteMatchHints(ScoredCandidate scored, TagProfile profile) {
        if (profile == null) {
            return List.of();
        }
        List<String> hints = new ArrayList<>();
        var likedGenres = profile.likedGenres();
        scored.candidate().allTags().stream()
            .map(tag -> tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT))
            .filter(tag -> tag.startsWith("genre:") && likedGenres.contains(tag))
            .map(this::genreAnalysisHint)
            .filter(value -> !value.isBlank())
            .forEach(hints::add);
        if (hints.isEmpty()) {
            hints.addAll(analysisHints(profile));
        }
        return hints.stream().distinct().limit(4).toList();
    }

    private String scheduleFit(kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate candidate) {
        List<String> parts = new ArrayList<>();
        if (candidate.startsAt() != null) {
            parts.add(DateTimeFormatter.ofPattern("MM-dd HH:mm").format(candidate.startsAt()));
        }
        if (!candidate.theaterName().isBlank()) {
            parts.add(candidate.theaterName());
        }
        String seat = seatSummary(candidate.remainingSeatCount(), candidate.totalSeatCount());
        if (!seat.isBlank()) {
            parts.add(seat);
        }
        if (candidate.runtimeMinutes() != null) {
            parts.add(candidate.runtimeMinutes() + "min");
        }
        return String.join(" / ", parts);
    }

    private List<String> tradeoffHints(ScoredCandidate scored, TagProfile profile) {
        var candidate = scored.candidate();
        List<String> hints = new ArrayList<>();
        List<String> risks = cautionHints(scored);
        if (!risks.isEmpty()) {
            hints.add("risk=" + String.join(" ", risks));
        }
        List<String> taste = tasteMatchHints(scored, profile);
        if (!taste.isEmpty()) {
            hints.add("taste=" + String.join(" ", taste));
        }
        String seat = seatHint(candidate.remainingSeatCount(), candidate.totalSeatCount());
        if ("limited".equals(seat)) {
            hints.add("practical=limited seats");
        } else if ("enough".equals(seat)) {
            hints.add("practical=seat-friendly");
        }
        if (candidate.runtimeMinutes() != null && candidate.runtimeMinutes() >= 140) {
            hints.add("runtime=long");
        }
        if (candidate.startsAt() != null) {
            hints.add("time=" + DateTimeFormatter.ofPattern("HH:mm").format(candidate.startsAt()));
        }
        return hints.stream().distinct().limit(5).toList();
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

    private List<String> cautionHints(ScoredCandidate scored) {
        var candidate = scored.candidate();
        List<String> hints = new ArrayList<>();
        scored.penalties().stream()
            .map(this::tagPhrase)
            .filter(value -> !value.isBlank())
            .forEach(hints::add);
        candidate.allTags().stream()
            .filter(tag -> tag != null && tag.trim().toLowerCase(Locale.ROOT).startsWith("content:"))
            .map(this::tagPhrase)
            .filter(value -> !value.isBlank())
            .forEach(hints::add);
        if (candidate.remainingSeatCount() != null && candidate.remainingSeatCount() <= 10) {
            hints.add("#좌석주의");
        }
        return hints.stream().distinct().limit(4).toList();
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

    private String seatSummary(Integer remainingSeatCount, Integer totalSeatCount) {
        if (remainingSeatCount == null) {
            return "";
        }
        if (totalSeatCount == null || totalSeatCount <= 0) {
            return remainingSeatCount + " seats left";
        }
        return remainingSeatCount + "/" + totalSeatCount + " seats left";
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

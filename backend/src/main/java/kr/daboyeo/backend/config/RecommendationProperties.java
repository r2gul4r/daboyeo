package kr.daboyeo.backend.config;

import java.util.List;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "daboyeo.recommendation")
public record RecommendationProperties(
    String lmStudioBaseUrl,
    String fastModel,
    String preciseModel,
    Integer minStartBufferMinutes,
    Integer fastAiCandidateLimit,
    Integer preciseAiCandidateLimit,
    Integer fastMaxTokens,
    Integer preciseMaxTokens,
    Integer responseTextMaxLength,
    List<String> frontendOrigins
) {

    public RecommendationProperties {
        lmStudioBaseUrl = defaultString(lmStudioBaseUrl, "http://127.0.0.1:1234/v1");
        fastModel = defaultString(fastModel, "gemma-4-e2b-it");
        preciseModel = defaultString(preciseModel, "gemma-4-e4b-it");
        minStartBufferMinutes = minStartBufferMinutes == null ? 20 : Math.max(0, minStartBufferMinutes);
        fastAiCandidateLimit = clamp(fastAiCandidateLimit, 5, 3, 8);
        preciseAiCandidateLimit = clamp(preciseAiCandidateLimit, 5, 3, 8);
        fastMaxTokens = clamp(fastMaxTokens, 280, 160, 420);
        preciseMaxTokens = clamp(preciseMaxTokens, 160, 96, 260);
        responseTextMaxLength = clamp(responseTextMaxLength, 56, 32, 96);
        if (frontendOrigins == null || frontendOrigins.isEmpty()) {
            frontendOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173", "http://*:5173");
        }
    }

    public String modelFor(RecommendationMode mode) {
        return mode == RecommendationMode.FAST ? fastModel : preciseModel;
    }

    public int aiCandidateLimitFor(RecommendationMode mode) {
        return mode == RecommendationMode.FAST ? fastAiCandidateLimit : preciseAiCandidateLimit;
    }

    public int maxTokensFor(RecommendationMode mode) {
        return mode == RecommendationMode.FAST ? fastMaxTokens : preciseMaxTokens;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int clamp(Integer value, int fallback, int min, int max) {
        int resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }
}

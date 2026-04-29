package kr.daboyeo.backend.config;

import java.util.List;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiProvider;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "daboyeo.recommendation")
public record RecommendationProperties(
    String lmStudioBaseUrl,
    String fastModel,
    String preciseModel,
    String gptBaseUrl,
    String gptModel,
    String gptFastReasoningEffort,
    String gptPreciseReasoningEffort,
    Integer gptFastAiCandidateLimit,
    Integer gptPreciseAiCandidateLimit,
    Integer gptFastMaxTokens,
    Integer gptPreciseMaxTokens,
    Integer minStartBufferMinutes,
    Integer fastAiCandidateLimit,
    Integer preciseAiCandidateLimit,
    Integer fastMaxTokens,
    Integer preciseMaxTokens,
    Integer responseTextMaxLength,
    List<String> frontendOrigins
) {

    public RecommendationProperties(
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
        this(
            lmStudioBaseUrl,
            fastModel,
            preciseModel,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            minStartBufferMinutes,
            fastAiCandidateLimit,
            preciseAiCandidateLimit,
            fastMaxTokens,
            preciseMaxTokens,
            responseTextMaxLength,
            frontendOrigins
        );
    }

    @ConstructorBinding
    public RecommendationProperties {
        lmStudioBaseUrl = defaultString(lmStudioBaseUrl, "http://127.0.0.1:1234/v1");
        fastModel = defaultString(fastModel, "gemma-4-e2b-it");
        preciseModel = defaultString(preciseModel, "gemma-4-e4b-it");
        gptBaseUrl = defaultString(gptBaseUrl, "http://127.0.0.1:10531/v1");
        gptModel = defaultString(gptModel, "gpt-5.5");
        gptFastReasoningEffort = defaultString(gptFastReasoningEffort, "low");
        gptPreciseReasoningEffort = defaultString(gptPreciseReasoningEffort, "high");
        gptFastAiCandidateLimit = clamp(gptFastAiCandidateLimit, 6, 3, 10);
        gptPreciseAiCandidateLimit = clamp(gptPreciseAiCandidateLimit, 8, 3, 12);
        gptFastMaxTokens = clamp(gptFastMaxTokens, 520, 320, 900);
        gptPreciseMaxTokens = clamp(gptPreciseMaxTokens, 900, 520, 1400);
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

    public String modelFor(AiProvider provider, RecommendationMode mode) {
        return provider == AiProvider.GPT ? gptModel : modelFor(mode);
    }

    public String baseUrlFor(AiProvider provider) {
        return provider == AiProvider.GPT ? gptBaseUrl : lmStudioBaseUrl;
    }

    public String reasoningEffortFor(AiProvider provider, RecommendationMode mode) {
        if (provider != AiProvider.GPT) {
            return "";
        }
        return mode == RecommendationMode.FAST ? gptFastReasoningEffort : gptPreciseReasoningEffort;
    }

    public String providerLabel(AiProvider provider) {
        return provider == AiProvider.GPT ? "GPT" : "로컬 Gemma";
    }

    public int aiCandidateLimitFor(RecommendationMode mode) {
        return mode == RecommendationMode.FAST ? fastAiCandidateLimit : preciseAiCandidateLimit;
    }

    public int aiCandidateLimitFor(AiProvider provider, RecommendationMode mode) {
        if (provider == AiProvider.GPT) {
            return mode == RecommendationMode.FAST ? gptFastAiCandidateLimit : gptPreciseAiCandidateLimit;
        }
        return aiCandidateLimitFor(mode);
    }

    public int maxTokensFor(RecommendationMode mode) {
        return mode == RecommendationMode.FAST ? fastMaxTokens : preciseMaxTokens;
    }

    public int maxTokensFor(AiProvider provider, RecommendationMode mode) {
        if (provider == AiProvider.GPT) {
            return mode == RecommendationMode.FAST ? gptFastMaxTokens : gptPreciseMaxTokens;
        }
        return maxTokensFor(mode);
    }

    public int responseTextMaxLengthFor(AiProvider provider, RecommendationMode mode) {
        if (provider == AiProvider.GPT) {
            return mode == RecommendationMode.FAST ? 140 : 220;
        }
        return responseTextMaxLength;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int clamp(Integer value, int fallback, int min, int max) {
        int resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }
}

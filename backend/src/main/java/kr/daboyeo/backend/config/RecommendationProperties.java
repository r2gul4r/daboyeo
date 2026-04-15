package kr.daboyeo.backend.config;

import java.util.List;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "daboyeo.recommendation")
public record RecommendationProperties(
    String ollamaBaseUrl,
    String fastModel,
    String preciseModel,
    List<String> frontendOrigins
) {

    public RecommendationProperties {
        ollamaBaseUrl = defaultString(ollamaBaseUrl, "http://127.0.0.1:11434");
        fastModel = fastModel == null ? "" : fastModel.trim();
        preciseModel = defaultString(preciseModel, "gemma4:e4b-it-q4_K_M");
        if (frontendOrigins == null || frontendOrigins.isEmpty()) {
            frontendOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173", "http://*:5173");
        }
    }

    public String modelFor(RecommendationMode mode) {
        return mode == RecommendationMode.FAST ? fastModel : preciseModel;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

package kr.daboyeo.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "daboyeo.cors")
public record BackendCorsProperties(List<String> allowedOrigins) {

    public BackendCorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of("http://localhost:5173", "http://localhost:8080") : List.copyOf(allowedOrigins);
    }
}

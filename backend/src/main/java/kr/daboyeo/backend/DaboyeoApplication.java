package kr.daboyeo.backend;

import kr.daboyeo.backend.config.RecommendationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RecommendationProperties.class)
public class DaboyeoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaboyeoApplication.class, args);
    }
}

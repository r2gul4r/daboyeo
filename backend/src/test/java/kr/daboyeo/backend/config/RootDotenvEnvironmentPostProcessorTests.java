package kr.daboyeo.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class RootDotenvEnvironmentPostProcessorTests {

    @Test
    void dotenvValuesAreAvailableWithoutOverridingExistingEnvironmentValues() throws IOException {
        Path root = createTempTree("root-dotenv-env");
        try {
            Path repo = Files.createDirectories(root.resolve("repo"));
            Path backend = Files.createDirectories(repo.resolve("backend"));
            Files.writeString(
                repo.resolve(".env"),
                """
                DABOYEO_DB_URL=jdbc:mysql://dotenv-host:4000/daboyeo
                DABOYEO_DB_USERNAME=dotenv-user
                """
            );

            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(
                new MapPropertySource(
                    "testOverrides",
                    Map.of("DABOYEO_DB_USERNAME", "os-user")
                )
            );

            String originalStartDir = System.getProperty("daboyeo.dotenv.start-dir");
            try {
                System.setProperty("daboyeo.dotenv.start-dir", backend.toString());
                new RootDotenvEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
            } finally {
                if (originalStartDir == null) {
                    System.clearProperty("daboyeo.dotenv.start-dir");
                } else {
                    System.setProperty("daboyeo.dotenv.start-dir", originalStartDir);
                }
            }

            assertThat(environment.getProperty("DABOYEO_DB_URL")).isEqualTo("jdbc:mysql://dotenv-host:4000/daboyeo");
            assertThat(environment.getProperty("DABOYEO_DB_USERNAME")).isEqualTo("os-user");
        } finally {
            deleteRecursively(root);
        }
    }

    private static Path createTempTree(String prefix) throws IOException {
        Path parent = Files.createDirectories(Path.of("build", "tmp", "tests"));
        return Files.createTempDirectory(parent, prefix);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        }
    }
}

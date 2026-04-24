package kr.daboyeo.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RootDotenvLoader {

    private RootDotenvLoader() {
    }

    public static Map<String, Object> load(Path startDirectory) {
        Path dotenvPath = findDotenvPath(startDirectory);
        if (dotenvPath == null) {
            return Map.of();
        }

        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(dotenvPath)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }
                values.put(key, stripQuotes(value));
            }
        } catch (IOException ignored) {
            return Map.of();
        }

        return values;
    }

    public static Path findDotenvPath(Path startDirectory) {
        Path current = startDirectory == null ? Path.of("").toAbsolutePath() : startDirectory.toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}

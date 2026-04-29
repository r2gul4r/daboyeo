package kr.daboyeo.backend.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import kr.daboyeo.backend.config.RootDotenvLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TheaterLocationEnricher {

    private static final Logger logger = LoggerFactory.getLogger(TheaterLocationEnricher.class);
    private static final TypeReference<List<Map<String, Object>>> LOCATION_ROWS = new TypeReference<>() {
    };

    private final Map<String, TheaterLocation> locationsByProviderAndCode;
    private final Map<String, TheaterLocation> locationsByProviderAndName;

    @Autowired
    public TheaterLocationEnricher(ObjectMapper objectMapper) {
        this(loadLocations(objectMapper));
    }

    TheaterLocationEnricher(List<TheaterLocation> locations) {
        Map<String, TheaterLocation> byCode = new LinkedHashMap<>();
        Map<String, TheaterLocation> byName = new LinkedHashMap<>();
        for (TheaterLocation location : locations) {
            if (!location.code().isBlank()) {
                byCode.put(providerKey(location.providerCode(), location.code()), location);
            }
            if (!location.name().isBlank()) {
                byName.put(providerKey(location.providerCode(), normalizeName(location.name())), location);
            }
        }
        this.locationsByProviderAndCode = Map.copyOf(byCode);
        this.locationsByProviderAndName = Map.copyOf(byName);
    }

    CollectorBundleIngestCommand.NormalizedBundle enrich(CollectorBundleIngestCommand.NormalizedBundle bundle) {
        List<CollectorBundleIngestCommand.TheaterRow> theaters = bundle.theaters().stream()
            .map(this::enrich)
            .toList();
        return new CollectorBundleIngestCommand.NormalizedBundle(
            bundle.movies(),
            theaters,
            bundle.screens(),
            bundle.showtimes()
        );
    }

    private CollectorBundleIngestCommand.TheaterRow enrich(CollectorBundleIngestCommand.TheaterRow theater) {
        if (theater.latitude() != null && theater.longitude() != null) {
            return theater;
        }

        TheaterLocation location = lookup(theater.providerCode(), theater.externalTheaterId(), theater.name());
        if (location == null) {
            return theater;
        }

        return new CollectorBundleIngestCommand.TheaterRow(
            theater.providerCode(),
            theater.externalTheaterId(),
            theater.name(),
            theater.regionCode(),
            theater.regionName(),
            blankToNull(theater.address()) == null ? location.address() : theater.address(),
            theater.latitude() == null ? location.latitude() : theater.latitude(),
            theater.longitude() == null ? location.longitude() : theater.longitude(),
            theater.raw()
        );
    }

    private TheaterLocation lookup(String providerCode, String externalTheaterId, String name) {
        TheaterLocation byCode = locationsByProviderAndCode.get(providerKey(providerCode, externalTheaterId));
        if (byCode != null) {
            return byCode;
        }
        return locationsByProviderAndName.get(providerKey(providerCode, normalizeName(name)));
    }

    private static List<TheaterLocation> loadLocations(ObjectMapper objectMapper) {
        Path theaterMapPath = resolveTheaterMapPath();
        if (theaterMapPath == null || !Files.isRegularFile(theaterMapPath)) {
            logger.warn("Theater map file was not found, so collector theater location enrichment is disabled.");
            return List.of();
        }

        try {
            List<Map<String, Object>> rows = objectMapper.readValue(theaterMapPath.toFile(), LOCATION_ROWS);
            List<TheaterLocation> locations = rows.stream()
                .map(TheaterLocationEnricher::toLocation)
                .filter(location -> location != null)
                .toList();
            logger.info("Loaded {} theater map rows for collector location enrichment.", locations.size());
            return locations;
        } catch (IOException exception) {
            logger.warn("Failed to load theater map for collector location enrichment from {}.", theaterMapPath, exception);
            return List.of();
        }
    }

    private static TheaterLocation toLocation(Map<String, Object> row) {
        String providerCode = normalizeProviderCode(text(row.get("provider")));
        String code = text(row.get("code"));
        String name = text(row.get("name"));
        BigDecimal latitude = decimalOrNull(row.get("lat"));
        BigDecimal longitude = decimalOrNull(row.get("lng"));
        String address = blankToNull(text(row.get("address")));
        if (providerCode.isBlank() || (code.isBlank() && name.isBlank()) || latitude == null || longitude == null) {
            return null;
        }
        return new TheaterLocation(providerCode, code, name, latitude, longitude, address);
    }

    private static Path resolveTheaterMapPath() {
        Path start = Path.of(System.getProperty("user.dir", "."));
        Path dotenv = RootDotenvLoader.findDotenvPath(start);
        Path workspaceRoot = dotenv != null && dotenv.getParent() != null ? dotenv.getParent() : start.toAbsolutePath();
        return workspaceRoot.resolve("frontend").resolve("src").resolve("map").resolve("theaters.json");
    }

    private static String normalizeProviderCode(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "LOTTE", "LOTTE_CINEMA" -> "LOTTE_CINEMA";
            case "MEGA", "MEGABOX" -> "MEGABOX";
            case "CGV" -> "CGV";
            default -> "";
        };
    }

    private static String providerKey(String providerCode, String value) {
        return normalizeProviderCode(blankToEmpty(providerCode)) + "::" + blankToEmpty(value);
    }

    private static String normalizeName(String value) {
        return blankToEmpty(value)
            .replace("메가박스", "")
            .replace("롯데시네마", "")
            .replace("CGV", "")
            .replaceAll("\\s+", "")
            .replaceAll("[()\\-_/]", "")
            .toUpperCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static BigDecimal decimalOrNull(Object value) {
        String text = text(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    record TheaterLocation(
        String providerCode,
        String code,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String address
    ) {
    }
}

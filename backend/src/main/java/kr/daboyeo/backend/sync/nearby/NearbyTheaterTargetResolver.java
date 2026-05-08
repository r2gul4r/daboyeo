package kr.daboyeo.backend.sync.nearby;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.config.RootDotenvLoader;
import kr.daboyeo.backend.domain.LiveMovieSearchCriteria;
import kr.daboyeo.backend.sync.bridge.CollectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NearbyTheaterTargetResolver {

    private static final Logger logger = LoggerFactory.getLogger(NearbyTheaterTargetResolver.class);
    private static final TypeReference<List<Map<String, Object>>> LOCATION_ROWS = new TypeReference<>() {
    };
    private static final double EARTH_RADIUS_KM = 6371.0d;

    private final CollectorSyncProperties properties;
    private final List<TheaterMapEntry> theaterMapEntries;

    @Autowired
    public NearbyTheaterTargetResolver(CollectorSyncProperties properties, ObjectMapper objectMapper) {
        this(properties, loadEntries(objectMapper));
    }

    NearbyTheaterTargetResolver(CollectorSyncProperties properties, List<TheaterMapEntry> theaterMapEntries) {
        this.properties = properties;
        this.theaterMapEntries = theaterMapEntries;
    }

    public Resolution resolve(LiveMovieSearchCriteria criteria) {
        Map<CollectorProvider, List<TheaterMapEntry>> grouped = new LinkedHashMap<>();
        double refreshRadiusKm = refreshRadiusKm(criteria);
        for (TheaterMapEntry entry : theaterMapEntries) {
            if (!supports(entry.provider())) {
                continue;
            }
            if (!matchesProviderFilter(criteria.providers(), entry.provider())) {
                continue;
            }
            double distanceKm = distanceKm(criteria.lat().doubleValue(), criteria.lng().doubleValue(), entry.latitude(), entry.longitude());
            if (distanceKm > refreshRadiusKm) {
                continue;
            }
            grouped.computeIfAbsent(entry.provider(), ignored -> new ArrayList<>())
                .add(entry.withDistanceKm(distanceKm));
        }

        int perProviderLimit = Math.max(1, properties.getShowtimes().getNearbyRefreshMaxTheatersPerProvider());
        List<TheaterMapEntry> lotteEntries = grouped.getOrDefault(CollectorProvider.LOTTE_CINEMA, List.of()).stream()
            .sorted(Comparator.comparingDouble(TheaterMapEntry::distanceKm))
            .limit(perProviderLimit)
            .toList();
        List<TheaterMapEntry> megaboxEntries = grouped.getOrDefault(CollectorProvider.MEGABOX, List.of()).stream()
            .sorted(Comparator.comparingDouble(TheaterMapEntry::distanceKm))
            .limit(perProviderLimit)
            .toList();

        return new Resolution(lotteEntries, megaboxEntries);
    }

    private static List<TheaterMapEntry> loadEntries(ObjectMapper objectMapper) {
        Path mapPath = resolveTheaterMapPath();
        if (mapPath == null || !Files.isRegularFile(mapPath)) {
            logger.warn("Theater map file was not found, so nearby refresh target resolution is disabled.");
            return List.of();
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(mapPath.toFile(), LOCATION_ROWS);
            return rows.stream()
                .map(NearbyTheaterTargetResolver::toEntry)
                .filter(entry -> entry != null)
                .toList();
        } catch (IOException exception) {
            logger.warn("Failed to load theater map for nearby refresh target resolution from {}.", mapPath, exception);
            return List.of();
        }
    }

    private static TheaterMapEntry toEntry(Map<String, Object> row) {
        CollectorProvider provider;
        try {
            provider = CollectorProvider.fromValue(text(row.get("provider")));
        } catch (IllegalArgumentException exception) {
            return null;
        }
        BigDecimal latitude = decimalOrNull(row.get("lat"));
        BigDecimal longitude = decimalOrNull(row.get("lng"));
        String code = text(row.get("code"));
        String name = text(row.get("name"));
        if (latitude == null || longitude == null || code.isBlank() || name.isBlank()) {
            return null;
        }
        return new TheaterMapEntry(provider, code, name, latitude.doubleValue(), longitude.doubleValue(), Double.MAX_VALUE);
    }

    private static Path resolveTheaterMapPath() {
        Path start = Path.of(System.getProperty("user.dir", "."));
        Path dotenv = RootDotenvLoader.findDotenvPath(start);
        Path workspaceRoot = dotenv != null && dotenv.getParent() != null ? dotenv.getParent() : start.toAbsolutePath();
        return workspaceRoot.resolve("frontend").resolve("src").resolve("map").resolve("theaters.json");
    }

    private static boolean supports(CollectorProvider provider) {
        return provider == CollectorProvider.LOTTE_CINEMA || provider == CollectorProvider.MEGABOX;
    }

    private double refreshRadiusKm(LiveMovieSearchCriteria criteria) {
        BigDecimal configuredRadius = properties.getShowtimes().getNearbyRefreshRadiusKm();
        if (configuredRadius == null || configuredRadius.signum() <= 0) {
            return criteria.radiusKm().doubleValue();
        }
        return Math.min(criteria.radiusKm().doubleValue(), configuredRadius.doubleValue());
    }

    private static boolean matchesProviderFilter(List<String> requestedProviders, CollectorProvider provider) {
        if (requestedProviders == null || requestedProviders.isEmpty()) {
            return true;
        }
        return requestedProviders.stream()
            .map(value -> value == null ? "" : value.trim().toUpperCase(Locale.ROOT))
            .map(value -> switch (value) {
                case "LOTTE", "LOTTE_CINEMA" -> "LOTTE_CINEMA";
                case "MEGA", "MEGABOX" -> "MEGABOX";
                default -> value;
            })
            .anyMatch(value -> value.equals(provider.name()));
    }

    private static double distanceKm(double sourceLat, double sourceLng, double targetLat, double targetLng) {
        double dLat = Math.toRadians(targetLat - sourceLat);
        double dLng = Math.toRadians(targetLng - sourceLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(sourceLat)) * Math.cos(Math.toRadians(targetLat))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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

    public record Resolution(
        List<TheaterMapEntry> lotteEntries,
        List<TheaterMapEntry> megaboxEntries
    ) {

        public boolean isEmpty() {
            return lotteEntries.isEmpty() && megaboxEntries.isEmpty();
        }
    }

    public record TheaterMapEntry(
        CollectorProvider provider,
        String externalTheaterId,
        String name,
        double latitude,
        double longitude,
        double distanceKm
    ) {
        TheaterMapEntry withDistanceKm(double value) {
            return new TheaterMapEntry(provider, externalTheaterId, name, latitude, longitude, value);
        }
    }
}

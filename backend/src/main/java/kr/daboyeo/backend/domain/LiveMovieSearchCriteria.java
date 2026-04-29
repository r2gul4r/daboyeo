package kr.daboyeo.backend.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record LiveMovieSearchCriteria(
    BigDecimal lat,
    BigDecimal lng,
    LocalDate date,
    LocalTime timeStart,
    LocalTime timeEnd,
    BigDecimal radiusKm,
    List<String> providers,
    List<String> formats,
    List<String> seatTypes,
    SeatState seatState,
    String query,
    int limit
) {

    private static final BigDecimal DEFAULT_RADIUS_KM = BigDecimal.valueOf(8);
    private static final int DEFAULT_LIMIT = 300;
    private static final int MAX_LIMIT = 500;

    public static LiveMovieSearchCriteria of(
        BigDecimal lat,
        BigDecimal lng,
        LocalDate date,
        LocalTime timeStart,
        LocalTime timeEnd,
        BigDecimal radiusKm,
        List<String> providers,
        List<String> formats,
        List<String> seatTypes,
        SeatState seatState,
        String query,
        Integer limit,
        Clock clock
    ) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("lat and lng are required.");
        }
        if (lat.doubleValue() < -90 || lat.doubleValue() > 90) {
            throw new IllegalArgumentException("lat must be between -90 and 90.");
        }
        if (lng.doubleValue() < -180 || lng.doubleValue() > 180) {
            throw new IllegalArgumentException("lng must be between -180 and 180.");
        }

        LocalDate resolvedDate = date == null ? LocalDate.now(clock) : date;
        LocalTime resolvedTimeStart = timeStart == null ? LocalTime.of(6, 0) : timeStart;
        LocalTime resolvedTimeEnd = timeEnd == null ? LocalTime.of(23, 59) : timeEnd;

        if (resolvedTimeStart.isAfter(resolvedTimeEnd)) {
            throw new IllegalArgumentException("timeStart must be earlier than or equal to timeEnd.");
        }

        BigDecimal resolvedRadiusKm = radiusKm == null ? DEFAULT_RADIUS_KM : radiusKm;
        if (resolvedRadiusKm.signum() <= 0 || resolvedRadiusKm.doubleValue() > 50) {
            throw new IllegalArgumentException("radiusKm must be greater than 0 and at most 50.");
        }

        int resolvedLimit = limit == null ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        if (resolvedLimit <= 0) {
            throw new IllegalArgumentException("limit must be at least 1.");
        }

        return new LiveMovieSearchCriteria(
            lat,
            lng,
            resolvedDate,
            resolvedTimeStart,
            resolvedTimeEnd,
            resolvedRadiusKm,
            normalizeList(providers),
            normalizeList(formats),
            normalizeList(seatTypes),
            seatState == null ? SeatState.ALL : seatState,
            query == null ? "" : query.trim(),
            resolvedLimit
        );
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .toList();
    }
}

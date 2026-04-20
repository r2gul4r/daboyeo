package kr.daboyeo.backend.repository.recommendation;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShowtimeRecommendationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShowtimeRecommendationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ShowtimeCandidate> findUpcomingCandidates(int limit, LocalDateTime minStartsAt) {
        String sql = """
            SELECT
              s.id AS showtime_id,
              s.movie_id AS movie_id,
              s.movie_title AS movie_title,
              s.provider_code AS provider_code,
              s.external_movie_id AS external_movie_id,
              s.theater_name AS theater_name,
              s.region_name AS region_name,
              s.screen_name AS screen_name,
              s.screen_type AS screen_type,
              s.format_name AS format_name,
              s.show_date AS show_date,
              s.starts_at AS starts_at,
              s.ends_at AS ends_at,
              s.remaining_seat_count AS remaining_seat_count,
              s.total_seat_count AS total_seat_count,
              s.min_price_amount AS min_price_amount,
              s.currency_code AS currency_code,
              s.booking_url AS booking_url,
              m.poster_url AS poster_url,
              m.age_rating AS age_rating,
              m.runtime_minutes AS runtime_minutes,
              mt.tag_type AS tag_type,
              mt.tag_value AS tag_value
            FROM showtimes s
            LEFT JOIN movies m
              ON m.id = s.movie_id
            LEFT JOIN movie_tags mt
              ON mt.provider_code = s.provider_code
             AND mt.external_movie_id = COALESCE(s.external_movie_id, m.external_movie_id)
            WHERE s.starts_at IS NOT NULL
              AND s.starts_at >= ?
            ORDER BY s.starts_at ASC, s.show_date ASC, s.id ASC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, rs -> {
            Map<Long, CandidateBuilder> builders = new LinkedHashMap<>();
            while (rs.next()) {
                long showtimeId = rs.getLong("showtime_id");
                CandidateBuilder builder = builders.computeIfAbsent(showtimeId, ignored -> new CandidateBuilder());
                builder.movieId = getNullableLong(rs.getLong("movie_id"), rs.wasNull());
                builder.showtimeId = showtimeId;
                builder.title = rs.getString("movie_title");
                builder.providerCode = rs.getString("provider_code");
                builder.externalMovieId = rs.getString("external_movie_id");
                builder.theaterName = rs.getString("theater_name");
                builder.regionName = rs.getString("region_name");
                builder.screenName = rs.getString("screen_name");
                builder.screenType = rs.getString("screen_type");
                builder.formatName = rs.getString("format_name");
                builder.showDate = rs.getObject("show_date", LocalDate.class);
                builder.startsAt = toLocalDateTime(rs.getTimestamp("starts_at"));
                builder.endsAt = toLocalDateTime(rs.getTimestamp("ends_at"));
                builder.remainingSeatCount = getNullableInt(rs.getInt("remaining_seat_count"), rs.wasNull());
                builder.totalSeatCount = getNullableInt(rs.getInt("total_seat_count"), rs.wasNull());
                builder.minPriceAmount = getNullableInt(rs.getInt("min_price_amount"), rs.wasNull());
                builder.currencyCode = rs.getString("currency_code");
                builder.bookingUrl = rs.getString("booking_url");
                builder.posterUrl = rs.getString("poster_url");
                builder.ageRating = rs.getString("age_rating");
                builder.runtimeMinutes = getNullableInt(rs.getInt("runtime_minutes"), rs.wasNull());
                String tagType = rs.getString("tag_type");
                String tagValue = rs.getString("tag_value");
                if (tagType != null && tagValue != null) {
                    builder.tags.add((tagType + ":" + tagValue).toLowerCase());
                }
            }
            return builders.values().stream().map(CandidateBuilder::build).toList();
        }, Timestamp.valueOf(minStartsAt), Math.max(1, limit));
    }

    public int countStoredShowtimes() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        return count == null ? 0 : count;
    }

    public Optional<ShowtimeCandidate> findByShowtimeId(Long showtimeId) {
        if (showtimeId == null) {
            return Optional.empty();
        }
        List<ShowtimeCandidate> candidates = jdbcTemplate.query(
            """
            SELECT
              s.id AS showtime_id,
              s.movie_id AS movie_id,
              s.movie_title AS movie_title,
              s.provider_code AS provider_code,
              s.external_movie_id AS external_movie_id,
              s.theater_name AS theater_name,
              s.region_name AS region_name,
              s.screen_name AS screen_name,
              s.screen_type AS screen_type,
              s.format_name AS format_name,
              s.show_date AS show_date,
              s.starts_at AS starts_at,
              s.ends_at AS ends_at,
              s.remaining_seat_count AS remaining_seat_count,
              s.total_seat_count AS total_seat_count,
              s.min_price_amount AS min_price_amount,
              s.currency_code AS currency_code,
              s.booking_url AS booking_url,
              m.poster_url AS poster_url,
              m.age_rating AS age_rating,
              m.runtime_minutes AS runtime_minutes,
              mt.tag_type AS tag_type,
              mt.tag_value AS tag_value
            FROM showtimes s
            LEFT JOIN movies m
              ON m.id = s.movie_id
            LEFT JOIN movie_tags mt
              ON mt.provider_code = s.provider_code
             AND mt.external_movie_id = COALESCE(s.external_movie_id, m.external_movie_id)
            WHERE s.id = ?
            """,
            rs -> {
                CandidateBuilder builder = null;
                while (rs.next()) {
                    if (builder == null) {
                        builder = new CandidateBuilder();
                    }
                    builder.movieId = getNullableLong(rs.getLong("movie_id"), rs.wasNull());
                    builder.showtimeId = rs.getLong("showtime_id");
                    builder.title = rs.getString("movie_title");
                    builder.providerCode = rs.getString("provider_code");
                    builder.externalMovieId = rs.getString("external_movie_id");
                    builder.theaterName = rs.getString("theater_name");
                    builder.regionName = rs.getString("region_name");
                    builder.screenName = rs.getString("screen_name");
                    builder.screenType = rs.getString("screen_type");
                    builder.formatName = rs.getString("format_name");
                    builder.showDate = rs.getObject("show_date", LocalDate.class);
                    builder.startsAt = toLocalDateTime(rs.getTimestamp("starts_at"));
                    builder.endsAt = toLocalDateTime(rs.getTimestamp("ends_at"));
                    builder.remainingSeatCount = getNullableInt(rs.getInt("remaining_seat_count"), rs.wasNull());
                    builder.totalSeatCount = getNullableInt(rs.getInt("total_seat_count"), rs.wasNull());
                    builder.minPriceAmount = getNullableInt(rs.getInt("min_price_amount"), rs.wasNull());
                    builder.currencyCode = rs.getString("currency_code");
                    builder.bookingUrl = rs.getString("booking_url");
                    builder.posterUrl = rs.getString("poster_url");
                    builder.ageRating = rs.getString("age_rating");
                    builder.runtimeMinutes = getNullableInt(rs.getInt("runtime_minutes"), rs.wasNull());
                    String tagType = rs.getString("tag_type");
                    String tagValue = rs.getString("tag_value");
                    if (tagType != null && tagValue != null) {
                        builder.tags.add((tagType + ":" + tagValue).toLowerCase());
                    }
                }
                return builder == null ? List.<ShowtimeCandidate>of() : List.of(builder.build());
            },
            showtimeId
        );
        return candidates.stream().findFirst();
    }

    private Long getNullableLong(long value, boolean wasNull) {
        return wasNull ? null : value;
    }

    private Integer getNullableInt(int value, boolean wasNull) {
        return wasNull ? null : value;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static final class CandidateBuilder {
        Long movieId;
        Long showtimeId;
        String title;
        String providerCode;
        String externalMovieId;
        String theaterName;
        String regionName;
        String screenName;
        String screenType;
        String formatName;
        LocalDate showDate;
        LocalDateTime startsAt;
        LocalDateTime endsAt;
        Integer remainingSeatCount;
        Integer totalSeatCount;
        Integer minPriceAmount;
        String currencyCode;
        String bookingUrl;
        String posterUrl;
        String ageRating;
        Integer runtimeMinutes;
        Set<String> tags = new LinkedHashSet<>();

        ShowtimeCandidate build() {
            return new ShowtimeCandidate(
                movieId,
                showtimeId,
                title,
                providerCode,
                externalMovieId,
                theaterName,
                regionName,
                screenName,
                screenType,
                formatName,
                showDate,
                startsAt,
                endsAt,
                remainingSeatCount,
                totalSeatCount,
                minPriceAmount,
                currencyCode,
                bookingUrl,
                posterUrl,
                ageRating,
                runtimeMinutes,
                tags
            );
        }
    }
}

package kr.daboyeo.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import kr.daboyeo.backend.domain.LiveMovieSchedule;
import kr.daboyeo.backend.domain.LiveMovieSearchCriteria;
import kr.daboyeo.backend.domain.SeatState;
import kr.daboyeo.backend.repository.LiveMovieRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class LiveMovieService {

    private final LiveMovieRepository repository;
    private final SeatStateCalculator seatStateCalculator;
    private final Clock clock;

    public LiveMovieService(LiveMovieRepository repository, SeatStateCalculator seatStateCalculator) {
        this.repository = repository;
        this.seatStateCalculator = seatStateCalculator;
        this.clock = Clock.system(ZoneId.of("Asia/Seoul"));
    }

    public LiveMovieResponse findNearby(LiveMovieSearchCriteria criteria) {
        try {
            List<LiveMovieScheduleItem> results = repository.findNearbySchedules(criteria).stream()
                .filter(schedule -> seatStateCalculator.matchesFilter(criteria.seatState(), schedule.totalSeatCount(), schedule.availableSeatCount()))
                .map(schedule -> toItem(schedule))
                .toList();

            return new LiveMovieResponse(
                new LiveMovieSearchMeta(
                    criteria.lat(),
                    criteria.lng(),
                    criteria.date().toString(),
                    criteria.timeStart().toString(),
                    criteria.timeEnd().toString(),
                    criteria.radiusKm(),
                    results.size(),
                    true,
                    null
                ),
                results
            );
        } catch (DataAccessException exception) {
            return new LiveMovieResponse(
                new LiveMovieSearchMeta(
                    criteria.lat(),
                    criteria.lng(),
                    criteria.date().toString(),
                    criteria.timeStart().toString(),
                    criteria.timeEnd().toString(),
                    criteria.radiusKm(),
                    0,
                    false,
                    "DB 조회에 실패해서 빈 결과를 반환했다."
                ),
                List.of()
            );
        }
    }

    public MovieSchedulesResponse findMovieSchedules(String movieKey, LiveMovieSearchCriteria criteria) {
        try {
            List<LiveMovieScheduleItem> items = repository.findMovieSchedules(movieKey, criteria).stream()
                .filter(schedule -> seatStateCalculator.matchesFilter(criteria.seatState(), schedule.totalSeatCount(), schedule.availableSeatCount()))
                .map(this::toItem)
                .toList();

            List<TheaterScheduleGroup> theaters = items.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    item -> item.provider() + "::" + item.theater_name(),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()
                ))
                .values().stream()
                .map(group -> {
                    LiveMovieScheduleItem first = group.get(0);
                    List<ScheduleCard> schedules = group.stream()
                        .sorted(Comparator.comparing(LiveMovieScheduleItem::start_time))
                        .map(item -> new ScheduleCard(
                            item.start_time(),
                            item.end_time(),
                            item.format_name(),
                            item.available_seat_count(),
                            item.total_seat_count(),
                            item.seat_state(),
                            item.booking_url()
                        ))
                        .toList();

                    return new TheaterScheduleGroup(
                        first.provider(),
                        first.provider_code(),
                        first.theater_id(),
                        first.theater_name(),
                        first.distance_km(),
                        schedules
                    );
                })
                .sorted(Comparator.comparing(TheaterScheduleGroup::theater_name).thenComparing(TheaterScheduleGroup::provider))
                .toList();

            MovieSummary movie = items.isEmpty()
                ? new MovieSummary(movieKey, null, null)
                : new MovieSummary(movieKey, items.get(0).movie_name(), items.get(0).age_rating());

            return new MovieSchedulesResponse(
                new LiveMovieSearchMeta(
                    criteria.lat(),
                    criteria.lng(),
                    criteria.date().toString(),
                    criteria.timeStart().toString(),
                    criteria.timeEnd().toString(),
                    criteria.radiusKm(),
                    items.size(),
                    true,
                    null
                ),
                movie,
                theaters
            );
        } catch (DataAccessException exception) {
            return new MovieSchedulesResponse(
                new LiveMovieSearchMeta(
                    criteria.lat(),
                    criteria.lng(),
                    criteria.date().toString(),
                    criteria.timeStart().toString(),
                    criteria.timeEnd().toString(),
                    criteria.radiusKm(),
                    0,
                    false,
                    "DB 조회에 실패해서 빈 결과를 반환했다."
                ),
                new MovieSummary(movieKey, null, null),
                List.of()
            );
        }
    }

    public LiveMovieSearchCriteria buildCriteria(
        BigDecimal lat,
        BigDecimal lng,
        java.time.LocalDate date,
        java.time.LocalTime timeStart,
        java.time.LocalTime timeEnd,
        BigDecimal radiusKm,
        List<String> providers,
        List<String> formats,
        List<String> seatTypes,
        SeatState seatState,
        String query,
        Integer limit
    ) {
        return LiveMovieSearchCriteria.of(
            lat,
            lng,
            date,
            timeStart,
            timeEnd,
            radiusKm,
            providers,
            formats,
            seatTypes,
            seatState,
            query,
            limit,
            clock
        );
    }

    private LiveMovieScheduleItem toItem(LiveMovieSchedule schedule) {
        SeatState seatState = seatStateCalculator.resolve(schedule.totalSeatCount(), schedule.availableSeatCount());
        BigDecimal seatRatio = seatStateCalculator.calculateRatio(schedule.totalSeatCount(), schedule.availableSeatCount());

        return new LiveMovieScheduleItem(
            schedule.movieKey(),
            schedule.movieName(),
            schedule.provider(),
            schedule.providerCode(),
            schedule.theaterId(),
            schedule.theaterName(),
            schedule.screenId(),
            schedule.screenName(),
            schedule.formatName(),
            schedule.seatTypeTags(),
            schedule.ageRating(),
            schedule.startTime(),
            schedule.endTime(),
            schedule.showDate().toString(),
            schedule.totalSeatCount(),
            schedule.availableSeatCount(),
            schedule.remainingSeatCount(),
            seatRatio,
            seatState.name().toLowerCase(),
            schedule.distanceKm() == null ? BigDecimal.ZERO : schedule.distanceKm().setScale(2, java.math.RoundingMode.HALF_UP),
            schedule.bookingUrl(),
            schedule.updatedAt() == null ? OffsetDateTime.now(clock).toString() : schedule.updatedAt().atOffset(OffsetDateTime.now(clock).getOffset()).toString()
        );
    }

    public record LiveMovieResponse(
        LiveMovieSearchMeta search,
        List<LiveMovieScheduleItem> results
    ) {
    }

    public record LiveMovieSearchMeta(
        BigDecimal lat,
        BigDecimal lng,
        String date,
        String timeStart,
        String timeEnd,
        BigDecimal radiusKm,
        int resultCount,
        boolean databaseAvailable,
        String warning
    ) {
    }

    public record LiveMovieScheduleItem(
        String movie_key,
        String movie_name,
        String provider,
        String provider_code,
        String theater_id,
        String theater_name,
        String screen_id,
        String screen_name,
        String format_name,
        List<String> seat_type_tags,
        String age_rating,
        String start_time,
        String end_time,
        String show_date,
        Integer total_seat_count,
        Integer available_seat_count,
        Integer remaining_seat_count,
        BigDecimal seat_ratio,
        String seat_state,
        BigDecimal distance_km,
        String booking_url,
        String updated_at
    ) {
    }

    public record MovieSchedulesResponse(
        LiveMovieSearchMeta search,
        MovieSummary movie,
        List<TheaterScheduleGroup> theaters
    ) {
    }

    public record MovieSummary(
        String movie_key,
        String movie_name,
        String age_rating
    ) {
    }

    public record TheaterScheduleGroup(
        String provider,
        String provider_code,
        String theater_id,
        String theater_name,
        BigDecimal distance_km,
        List<ScheduleCard> schedules
    ) {
    }

    public record ScheduleCard(
        String start_time,
        String end_time,
        String format_name,
        Integer available_seat_count,
        Integer total_seat_count,
        String seat_state,
        String booking_url
    ) {
    }
}

package kr.daboyeo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import kr.daboyeo.backend.domain.LiveMovieSearchCriteria;
import kr.daboyeo.backend.domain.SeatState;
import kr.daboyeo.backend.repository.LiveMovieRepository;
import kr.daboyeo.backend.sync.nearby.NearbyShowtimeRefreshService;
import org.junit.jupiter.api.Test;

class LiveMovieServiceNearbyRefreshTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void nearbySearchTriggersBackgroundRefreshWithoutBlockingResponse() {
        LiveMovieRepository repository = mock(LiveMovieRepository.class);
        NearbyShowtimeRefreshService refreshService = mock(NearbyShowtimeRefreshService.class);
        LiveMovieSearchCriteria criteria = sampleCriteria();
        when(repository.findNearbySchedules(criteria)).thenReturn(List.of());

        LiveMovieService service = new LiveMovieService(
            repository,
            new SeatStateCalculator(),
            new LiveMovieDemoDataService(),
            refreshService,
            false
        );

        LiveMovieService.LiveMovieResponse response = service.findNearby(criteria);

        verify(refreshService, times(1)).requestRefresh(criteria);
        assertThat(response.search().databaseAvailable()).isTrue();
    }

    @Test
    void nearbySearchIgnoresRefreshFailures() {
        LiveMovieRepository repository = mock(LiveMovieRepository.class);
        NearbyShowtimeRefreshService refreshService = mock(NearbyShowtimeRefreshService.class);
        LiveMovieSearchCriteria criteria = sampleCriteria();
        when(repository.findNearbySchedules(criteria)).thenReturn(List.of());
        doThrow(new IllegalStateException("refresh failed")).when(refreshService).requestRefresh(criteria);

        LiveMovieService service = new LiveMovieService(
            repository,
            new SeatStateCalculator(),
            new LiveMovieDemoDataService(),
            refreshService,
            false
        );

        LiveMovieService.LiveMovieResponse response = service.findNearby(criteria);

        assertThat(response.search().databaseAvailable()).isTrue();
    }

    private static LiveMovieSearchCriteria sampleCriteria() {
        return LiveMovieSearchCriteria.of(
            new BigDecimal("37.4979"),
            new BigDecimal("127.0276"),
            LocalDate.of(2026, 4, 30),
            LocalTime.of(6, 0),
            LocalTime.of(23, 59),
            new BigDecimal("8"),
            List.of(),
            List.of(),
            List.of(),
            SeatState.ALL,
            "",
            300,
            FIXED_CLOCK
        );
    }
}

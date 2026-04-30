package kr.daboyeo.backend.sync.nearby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.domain.LiveMovieSearchCriteria;
import kr.daboyeo.backend.domain.SeatState;
import kr.daboyeo.backend.ingest.CollectorBundleIngestCommand;
import kr.daboyeo.backend.ingest.CollectorBundlePersistenceService;
import kr.daboyeo.backend.sync.bridge.CollectorProvider;
import kr.daboyeo.backend.sync.bridge.PythonCollectorBridge;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

class NearbyShowtimeRefreshServiceTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void nearbyRefreshPersistsLotteTargetsWhenStale() {
        CollectorSyncProperties properties = sampleProperties();
        NearbyTheaterTargetResolver resolver = mock(NearbyTheaterTargetResolver.class);
        NearbyRefreshRepository repository = mock(NearbyRefreshRepository.class);
        PythonCollectorBridge bridge = mock(PythonCollectorBridge.class);
        CollectorBundlePersistenceService persistenceService = mock(CollectorBundlePersistenceService.class);
        LiveMovieSearchCriteria criteria = sampleCriteria();

        when(resolver.resolve(criteria)).thenReturn(new NearbyTheaterTargetResolver.Resolution(
            List.of(new NearbyTheaterTargetResolver.TheaterMapEntry(CollectorProvider.LOTTE_CINEMA, "1003", "노원", 37.0d, 127.0d, 1.0d)),
            List.of()
        ));
        when(repository.findTheaterSyncMetadata(eq(CollectorProvider.LOTTE_CINEMA), any()))
            .thenReturn(List.of(new NearbyRefreshRepository.TheaterSyncMetadata(
                CollectorProvider.LOTTE_CINEMA,
                "1003",
                "노원",
                "2|0986|1003",
                ""
            )));
        when(repository.findLatestShowtimeCollectedAt(eq(CollectorProvider.LOTTE_CINEMA), eq(criteria.date()), any()))
            .thenReturn(Map.of("1003", LocalDateTime.of(2026, 4, 29, 0, 0)));
        when(bridge.collectLotteNearbyDiscovery(eq(criteria.date()), eq("2|0986|1003")))
            .thenReturn(new PythonCollectorBridge.ProviderDiscoveryPayload(List.of(
                Map.of("cinema_id", "1003", "cinema_selector", "2|0986|1003", "representation_movie_code", "L100")
            )));
        when(bridge.collectShowtimeBundle(any())).thenReturn(Map.of());
        when(persistenceService.persist(eq("LOTTE_CINEMA"), any(), eq(false)))
            .thenReturn(new CollectorBundleIngestCommand.IngestResult(1, 1, 1, 1));

        NearbyShowtimeRefreshService service = new NearbyShowtimeRefreshService(
            properties,
            resolver,
            repository,
            bridge,
            persistenceService,
            new SyncTaskExecutor(),
            FIXED_CLOCK
        );

        service.requestRefresh(criteria);

        verify(bridge, times(1)).collectLotteNearbyDiscovery(criteria.date(), "2|0986|1003");
        verify(bridge, times(1)).collectShowtimeBundle(any());
        verify(persistenceService, times(1)).persist(eq("LOTTE_CINEMA"), any(), eq(false));
    }

    @Test
    void nearbyRefreshSkipsFreshMegaboxTheaters() {
        CollectorSyncProperties properties = sampleProperties();
        NearbyTheaterTargetResolver resolver = mock(NearbyTheaterTargetResolver.class);
        NearbyRefreshRepository repository = mock(NearbyRefreshRepository.class);
        PythonCollectorBridge bridge = mock(PythonCollectorBridge.class);
        CollectorBundlePersistenceService persistenceService = mock(CollectorBundlePersistenceService.class);
        LiveMovieSearchCriteria criteria = sampleCriteria();

        when(resolver.resolve(criteria)).thenReturn(new NearbyTheaterTargetResolver.Resolution(
            List.of(),
            List.of(new NearbyTheaterTargetResolver.TheaterMapEntry(CollectorProvider.MEGABOX, "1372", "강남", 37.0d, 127.0d, 1.0d))
        ));
        when(repository.findTheaterSyncMetadata(eq(CollectorProvider.MEGABOX), any()))
            .thenReturn(List.of(new NearbyRefreshRepository.TheaterSyncMetadata(
                CollectorProvider.MEGABOX,
                "1372",
                "강남",
                "",
                "11"
            )));
        when(repository.findLatestShowtimeCollectedAt(eq(CollectorProvider.MEGABOX), eq(criteria.date()), any()))
            .thenReturn(Map.of("1372", LocalDateTime.of(2026, 4, 30, 8, 30)));

        NearbyShowtimeRefreshService service = new NearbyShowtimeRefreshService(
            properties,
            resolver,
            repository,
            bridge,
            persistenceService,
            new SyncTaskExecutor(),
            FIXED_CLOCK
        );

        service.requestRefresh(criteria);

        verify(bridge, never()).collectMegaboxNearbyDiscovery(any(), any());
        verify(bridge, never()).collectShowtimeBundle(any());
    }

    @Test
    void nearbyRefreshPersistsMegaboxTargetsWhenStale() {
        CollectorSyncProperties properties = sampleProperties();
        NearbyTheaterTargetResolver resolver = mock(NearbyTheaterTargetResolver.class);
        NearbyRefreshRepository repository = mock(NearbyRefreshRepository.class);
        PythonCollectorBridge bridge = mock(PythonCollectorBridge.class);
        CollectorBundlePersistenceService persistenceService = mock(CollectorBundlePersistenceService.class);
        LiveMovieSearchCriteria criteria = sampleCriteria();

        when(resolver.resolve(criteria)).thenReturn(new NearbyTheaterTargetResolver.Resolution(
            List.of(),
            List.of(new NearbyTheaterTargetResolver.TheaterMapEntry(CollectorProvider.MEGABOX, "1372", "강남", 37.0d, 127.0d, 1.0d))
        ));
        when(repository.findTheaterSyncMetadata(eq(CollectorProvider.MEGABOX), any()))
            .thenReturn(List.of(new NearbyRefreshRepository.TheaterSyncMetadata(
                CollectorProvider.MEGABOX,
                "1372",
                "강남",
                "",
                "11"
            )));
        when(repository.findLatestShowtimeCollectedAt(eq(CollectorProvider.MEGABOX), eq(criteria.date()), any()))
            .thenReturn(Map.of("1372", LocalDateTime.of(2026, 4, 29, 0, 0)));
        when(bridge.collectMegaboxNearbyDiscovery(eq(criteria.date()), eq("11")))
            .thenReturn(new PythonCollectorBridge.ProviderDiscoveryPayload(List.of(
                Map.of("movie_no", "M100", "area_code", "11")
            )));
        when(bridge.collectShowtimeBundle(any())).thenReturn(Map.of());
        when(persistenceService.persist(eq("MEGABOX"), any(), eq(false)))
            .thenReturn(new CollectorBundleIngestCommand.IngestResult(1, 2, 3, 4));

        NearbyShowtimeRefreshService service = new NearbyShowtimeRefreshService(
            properties,
            resolver,
            repository,
            bridge,
            persistenceService,
            new SyncTaskExecutor(),
            FIXED_CLOCK
        );

        service.requestRefresh(criteria);

        verify(bridge, times(1)).collectMegaboxNearbyDiscovery(criteria.date(), "11");
        verify(bridge, times(1)).collectShowtimeBundle(any());
        verify(persistenceService, times(1)).persist(eq("MEGABOX"), any(), eq(false));
    }

    private static CollectorSyncProperties sampleProperties() {
        CollectorSyncProperties properties = new CollectorSyncProperties();
        properties.setEnabled(true);
        properties.setTimezone("Asia/Seoul");
        properties.getShowtimes().setEnabled(true);
        properties.getShowtimes().setNearbyRefreshEnabled(true);
        properties.getShowtimes().setNearbyRefreshMaxTheatersPerProvider(6);
        properties.getShowtimes().setNearbyRefreshTodayTtlMinutes(60);
        properties.getShowtimes().setNearbyRefreshNextDayTtlMinutes(360);
        properties.getShowtimes().setNearbyRefreshFutureTtlMinutes(1440);
        return properties;
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

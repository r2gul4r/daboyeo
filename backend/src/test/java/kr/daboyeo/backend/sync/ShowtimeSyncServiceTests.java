package kr.daboyeo.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.ingest.CollectorBundleIngestCommand;
import kr.daboyeo.backend.ingest.CollectorBundlePersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShowtimeSyncServiceTests {

    @Test
    void syncsEveryConfiguredProviderAndDateOffset() {
        CollectorSyncProperties properties = new CollectorSyncProperties();
        properties.setEnabled(true);
        properties.setTimezone("Asia/Seoul");
        properties.getShowtimes().setEnabled(true);
        properties.getShowtimes().setDateOffsetDays(List.of(0, 1));

        CollectorSyncProperties.CgvTarget cgvTarget = new CollectorSyncProperties.CgvTarget();
        cgvTarget.setSiteNo("0013");
        cgvTarget.setMovieNo("20042000");
        properties.getShowtimes().setCgvTargets(List.of(cgvTarget));

        CollectorSyncProperties.LotteTarget lotteTarget = new CollectorSyncProperties.LotteTarget();
        lotteTarget.setCinemaSelector("1|101|0001");
        lotteTarget.setRepresentationMovieCode("L100");
        properties.getShowtimes().setLotteTargets(List.of(lotteTarget));

        CollectorSyncProperties.MegaboxTarget megaboxTarget = new CollectorSyncProperties.MegaboxTarget();
        megaboxTarget.setMovieNo("240001");
        megaboxTarget.setAreaCode("11");
        properties.getShowtimes().setMegaboxTargets(List.of(megaboxTarget));

        PythonCollectorBridge bridge = mock(PythonCollectorBridge.class);
        when(bridge.collectShowtimeBundle(any())).thenReturn(Map.of());

        CollectorBundlePersistenceService persistenceService = mock(CollectorBundlePersistenceService.class);
        when(persistenceService.persist(any(), any(), eq(false))).thenReturn(new CollectorBundleIngestCommand.IngestResult(1, 1, 1, 1));

        ShowtimeSyncService service = new ShowtimeSyncService(properties, bridge, persistenceService);
        service.syncDailyShowtimes();

        ArgumentCaptor<ShowtimeCollectionRequest> requestCaptor = ArgumentCaptor.forClass(ShowtimeCollectionRequest.class);
        verify(bridge, times(6)).collectShowtimeBundle(requestCaptor.capture());
        verify(persistenceService, times(6)).persist(any(), any(), eq(false));

        List<ShowtimeCollectionRequest> requests = new ArrayList<>(requestCaptor.getAllValues());
        assertThat(requests).extracting(ShowtimeCollectionRequest::provider)
            .containsExactlyInAnyOrder(
                CollectorProvider.CGV,
                CollectorProvider.CGV,
                CollectorProvider.LOTTE_CINEMA,
                CollectorProvider.LOTTE_CINEMA,
                CollectorProvider.MEGABOX,
                CollectorProvider.MEGABOX
            );
        assertThat(requests).extracting(ShowtimeCollectionRequest::playDate).hasSize(6).allSatisfy(date -> assertThat(date).isAfterOrEqualTo(LocalDate.now().minusDays(1)));
    }
}

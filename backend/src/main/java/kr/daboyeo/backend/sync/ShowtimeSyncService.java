package kr.daboyeo.backend.sync;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.ingest.CollectorBundleIngestCommand;
import kr.daboyeo.backend.ingest.CollectorBundlePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShowtimeSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ShowtimeSyncService.class);

    private final CollectorSyncProperties properties;
    private final PythonCollectorBridge collectorBridge;
    private final CollectorBundlePersistenceService persistenceService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ShowtimeSyncService(
        CollectorSyncProperties properties,
        PythonCollectorBridge collectorBridge,
        CollectorBundlePersistenceService persistenceService
    ) {
        this.properties = properties;
        this.collectorBridge = collectorBridge;
        this.persistenceService = persistenceService;
    }

    public void syncDailyShowtimes() {
        if (!properties.isEnabled() || !properties.getShowtimes().isEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("Showtime sync is already running. Skipping overlapping trigger.");
            return;
        }

        try {
            LocalDate baseDate = LocalDate.now(ZoneId.of(properties.getTimezone()));
            for (Integer offset : properties.getShowtimes().getDateOffsetDays()) {
                LocalDate playDate = baseDate.plusDays(offset == null ? 0 : offset);
                syncCgv(playDate);
                syncLotte(playDate);
                syncMegabox(playDate);
            }
        } finally {
            running.set(false);
        }
    }

    private void syncCgv(LocalDate playDate) {
        for (CollectorSyncProperties.CgvTarget target : properties.getShowtimes().getCgvTargets()) {
            if (isBlank(target.getSiteNo()) || isBlank(target.getMovieNo())) {
                continue;
            }
            ShowtimeCollectionRequest request = new ShowtimeCollectionRequest(
                CollectorProvider.CGV,
                playDate,
                target.getSiteNo(),
                target.getMovieNo(),
                null,
                null,
                null
            );
            persistCollectedBundle(request);
        }
    }

    private void syncLotte(LocalDate playDate) {
        for (CollectorSyncProperties.LotteTarget target : properties.getShowtimes().getLotteTargets()) {
            if (isBlank(target.getCinemaSelector()) || isBlank(target.getRepresentationMovieCode())) {
                continue;
            }
            ShowtimeCollectionRequest request = new ShowtimeCollectionRequest(
                CollectorProvider.LOTTE_CINEMA,
                playDate,
                null,
                null,
                target.getCinemaSelector(),
                target.getRepresentationMovieCode(),
                null
            );
            persistCollectedBundle(request);
        }
    }

    private void syncMegabox(LocalDate playDate) {
        for (CollectorSyncProperties.MegaboxTarget target : properties.getShowtimes().getMegaboxTargets()) {
            if (isBlank(target.getMovieNo()) || isBlank(target.getAreaCode())) {
                continue;
            }
            ShowtimeCollectionRequest request = new ShowtimeCollectionRequest(
                CollectorProvider.MEGABOX,
                playDate,
                null,
                target.getMovieNo(),
                null,
                null,
                target.getAreaCode()
            );
            persistCollectedBundle(request);
        }
    }

    private void persistCollectedBundle(ShowtimeCollectionRequest request) {
        try {
            Map<String, Object> bundle = collectorBridge.collectShowtimeBundle(request);
            CollectorBundleIngestCommand.IngestResult result = persistenceService.persist(request.provider().name(), bundle, false);
            logger.info(
                "Showtime sync stored provider={} date={} movies={} theaters={} screens={} showtimes={}",
                request.provider(),
                request.playDate(),
                result.movies(),
                result.theaters(),
                result.screens(),
                result.showtimes()
            );
        } catch (Exception exception) {
            logger.error("Showtime sync failed for provider={} date={}", request.provider(), request.playDate(), exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package kr.daboyeo.backend.sync.showtime;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.ingest.CollectorBundleIngestCommand;
import kr.daboyeo.backend.ingest.CollectorBundlePersistenceService;
import kr.daboyeo.backend.sync.bridge.CollectorProvider;
import kr.daboyeo.backend.sync.bridge.PythonCollectorBridge;
import kr.daboyeo.backend.sync.bridge.ShowtimeCollectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShowtimeSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ShowtimeSyncService.class);

    private final CollectorSyncProperties properties;
    private final PythonCollectorBridge collectorBridge;
    private final CollectorBundlePersistenceService persistenceService;
    private final ShowtimeCleanupService cleanupService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ShowtimeSyncService(
        CollectorSyncProperties properties,
        PythonCollectorBridge collectorBridge,
        CollectorBundlePersistenceService persistenceService,
        ShowtimeCleanupService cleanupService
    ) {
        this.properties = properties;
        this.collectorBridge = collectorBridge;
        this.persistenceService = persistenceService;
        this.cleanupService = cleanupService;
    }

    public void syncDailyShowtimes() {
        if (!properties.isEnabled()) {
            logger.info("Showtime sync skipped because daboyeo.sync.enabled=false.");
            return;
        }
        if (!properties.getShowtimes().isEnabled()) {
            logger.info("Showtime sync skipped because daboyeo.sync.showtimes.enabled=false.");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("Showtime sync is already running. Skipping overlapping trigger.");
            return;
        }

        try {
            LocalDate baseDate = LocalDate.now(ZoneId.of(properties.getTimezone()));
            logger.info(
                "Showtime sync starting timezone={} baseDate={} offsets={} cgvTargets={} lotteTargets={} megaboxTargets={}",
                properties.getTimezone(),
                baseDate,
                properties.getShowtimes().getDateOffsetDays(),
                validCgvTargets(),
                validLotteTargets(),
                validMegaboxTargets()
            );
            for (Integer offset : properties.getShowtimes().getDateOffsetDays()) {
                LocalDate playDate = baseDate.plusDays(offset == null ? 0 : offset);
                syncCgv(playDate);
                syncLotte(playDate);
                syncMegabox(playDate);
                if (properties.getShowtimes().isAutoDiscoveryEnabled()) {
                    syncLotteDiscovered(playDate);
                    syncMegaboxDiscovered(playDate);
                }
            }
            cleanupExpiredShowtimes(baseDate);
            logger.info("Showtime sync completed for baseDate={}.", baseDate);
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
        persistCollectedBundleWithResult(request);
    }

    private CollectorBundleIngestCommand.IngestResult persistCollectedBundleWithResult(ShowtimeCollectionRequest request) {
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
            return result;
        } catch (Exception exception) {
            if (request.provider() == CollectorProvider.CGV && exception.getMessage() != null && exception.getMessage().contains("401")) {
                logger.warn(
                    "CGV collector is blocked by upstream 401 for siteNo={} movieNo={} date={}.",
                    request.siteNo(),
                    request.movieNo(),
                    request.playDate()
                );
            }
            logger.error("Showtime sync failed for provider={} date={}", request.provider(), request.playDate(), exception);
            return new CollectorBundleIngestCommand.IngestResult(0, 0, 0, 0);
        }
    }

    private void syncLotteDiscovered(LocalDate playDate) {
        logger.info(
            "Showtime auto-discovery starting provider={} date={} movieLimit={} cinemaLimit={} perCinemaLimit={} totalTargetLimit={}",
            CollectorProvider.LOTTE_CINEMA,
            playDate,
            properties.getShowtimes().getDiscoveryMovieLimit(),
            properties.getShowtimes().getDiscoveryLotteCinemaLimit(),
            properties.getShowtimes().getDiscoveryLotteMovieTargetLimit(),
            properties.getShowtimes().getDiscoveryLotteTotalTargetLimit()
        );
        PythonCollectorBridge.ProviderDiscoveryPayload discovery;
        try {
            discovery = collectorBridge.collectShowtimeDiscovery(CollectorProvider.LOTTE_CINEMA, playDate);
        } catch (Exception exception) {
            logger.error("Showtime auto-discovery failed for provider={} date={}", CollectorProvider.LOTTE_CINEMA, playDate, exception);
            return;
        }
        if (discovery.targets().isEmpty()) {
            logger.info("Showtime auto-discovery found no working LOTTE_CINEMA targets for date={}.", playDate);
            return;
        }
        logger.info(
            "Showtime auto-discovery found {} LOTTE_CINEMA targets for date={}.",
            discovery.targets().size(),
            playDate
        );
        for (Map<String, Object> target : discovery.targets()) {
            ShowtimeCollectionRequest request = new ShowtimeCollectionRequest(
                CollectorProvider.LOTTE_CINEMA,
                playDate,
                null,
                null,
                text(target.get("cinema_selector")),
                text(target.get("representation_movie_code")),
                null
            );
            persistCollectedBundleWithResult(request);
        }
    }

    private void syncMegaboxDiscovered(LocalDate playDate) {
        logger.info(
            "Showtime auto-discovery starting provider={} date={} movieLimit={} bundleLimit={}",
            CollectorProvider.MEGABOX,
            playDate,
            properties.getShowtimes().getDiscoveryMovieLimit(),
            properties.getShowtimes().getDiscoveryMegaboxBundleLimit()
        );
        PythonCollectorBridge.ProviderDiscoveryPayload discovery;
        try {
            discovery = collectorBridge.collectShowtimeDiscovery(CollectorProvider.MEGABOX, playDate);
        } catch (Exception exception) {
            logger.error("Showtime auto-discovery failed for provider={} date={}", CollectorProvider.MEGABOX, playDate, exception);
            return;
        }
        if (discovery.targets().isEmpty()) {
            logger.info("Showtime auto-discovery found no working MEGABOX targets for date={}.", playDate);
            return;
        }
        logger.info(
            "Showtime auto-discovery found {} MEGABOX targets for date={}.",
            discovery.targets().size(),
            playDate
        );
        for (Map<String, Object> target : discovery.targets()) {
            ShowtimeCollectionRequest request = new ShowtimeCollectionRequest(
                CollectorProvider.MEGABOX,
                playDate,
                null,
                text(target.get("movie_no")),
                null,
                null,
                text(target.get("area_code"))
            );
            persistCollectedBundleWithResult(request);
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void cleanupExpiredShowtimes(LocalDate baseDate) {
        if (!properties.getShowtimes().isCleanupEnabled()) {
            return;
        }
        ShowtimeCleanupRepository.CleanupCounts counts = cleanupService.cleanupForBaseDate(baseDate);
        logger.info(
            "Showtime cleanup completed for baseDate={} deletedShowtimes={} deletedSeatSnapshots={} deletedSeatSnapshotItems={}",
            baseDate,
            counts.deletedShowtimes(),
            counts.deletedSeatSnapshots(),
            counts.deletedSeatSnapshotItems()
        );
    }

    private long validCgvTargets() {
        return properties.getShowtimes().getCgvTargets().stream()
            .filter(target -> !isBlank(target.getSiteNo()) && !isBlank(target.getMovieNo()))
            .count();
    }

    private long validLotteTargets() {
        return properties.getShowtimes().getLotteTargets().stream()
            .filter(target -> !isBlank(target.getCinemaSelector()) && !isBlank(target.getRepresentationMovieCode()))
            .count();
    }

    private long validMegaboxTargets() {
        return properties.getShowtimes().getMegaboxTargets().stream()
            .filter(target -> !isBlank(target.getMovieNo()) && !isBlank(target.getAreaCode()))
            .count();
    }
}

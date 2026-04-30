package kr.daboyeo.backend.sync.nearby;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.domain.LiveMovieSearchCriteria;
import kr.daboyeo.backend.ingest.CollectorBundleIngestCommand;
import kr.daboyeo.backend.ingest.CollectorBundlePersistenceService;
import kr.daboyeo.backend.sync.bridge.CollectorProvider;
import kr.daboyeo.backend.sync.bridge.PythonCollectorBridge;
import kr.daboyeo.backend.sync.bridge.ShowtimeCollectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class NearbyShowtimeRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(NearbyShowtimeRefreshService.class);

    private final CollectorSyncProperties properties;
    private final NearbyTheaterTargetResolver resolver;
    private final NearbyRefreshRepository repository;
    private final PythonCollectorBridge collectorBridge;
    private final CollectorBundlePersistenceService persistenceService;
    private final TaskExecutor taskExecutor;
    private final Clock clock;
    private final Set<String> inFlightKeys = ConcurrentHashMap.newKeySet();

    @Autowired
    public NearbyShowtimeRefreshService(
        CollectorSyncProperties properties,
        NearbyTheaterTargetResolver resolver,
        NearbyRefreshRepository repository,
        PythonCollectorBridge collectorBridge,
        CollectorBundlePersistenceService persistenceService,
        @Qualifier("nearbyRefreshTaskExecutor") TaskExecutor taskExecutor
    ) {
        this(properties, resolver, repository, collectorBridge, persistenceService, taskExecutor, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    NearbyShowtimeRefreshService(
        CollectorSyncProperties properties,
        NearbyTheaterTargetResolver resolver,
        NearbyRefreshRepository repository,
        PythonCollectorBridge collectorBridge,
        CollectorBundlePersistenceService persistenceService,
        TaskExecutor taskExecutor,
        Clock clock
    ) {
        this.properties = properties;
        this.resolver = resolver;
        this.repository = repository;
        this.collectorBridge = collectorBridge;
        this.persistenceService = persistenceService;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    public void requestRefresh(LiveMovieSearchCriteria criteria) {
        if (!properties.isEnabled() || !properties.getShowtimes().isEnabled() || !properties.getShowtimes().isNearbyRefreshEnabled()) {
            return;
        }
        NearbyTheaterTargetResolver.Resolution resolution = resolver.resolve(criteria);
        if (resolution.isEmpty()) {
            return;
        }
        logger.info(
            "Nearby refresh requested date={} lotteCandidates={} megaboxCandidates={} radiusKm={}",
            criteria.date(),
            resolution.lotteEntries().size(),
            resolution.megaboxEntries().size(),
            criteria.radiusKm()
        );
        taskExecutor.execute(() -> refreshNearby(criteria, resolution));
    }

    private void refreshNearby(LiveMovieSearchCriteria criteria, NearbyTheaterTargetResolver.Resolution resolution) {
        try {
            refreshLotte(criteria.date(), resolution.lotteEntries());
        } catch (Exception exception) {
            logger.warn("Nearby refresh failed for provider={} date={}", CollectorProvider.LOTTE_CINEMA, criteria.date(), exception);
        }
        try {
            refreshMegabox(criteria.date(), resolution.megaboxEntries());
        } catch (Exception exception) {
            logger.warn("Nearby refresh failed for provider={} date={}", CollectorProvider.MEGABOX, criteria.date(), exception);
        }
    }

    private void refreshLotte(LocalDate showDate, List<NearbyTheaterTargetResolver.TheaterMapEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<NearbyRefreshRepository.TheaterSyncMetadata> metadata = repository.findTheaterSyncMetadata(
            CollectorProvider.LOTTE_CINEMA,
            entries.stream().map(NearbyTheaterTargetResolver.TheaterMapEntry::externalTheaterId).toList()
        );
        Map<String, NearbyRefreshRepository.TheaterSyncMetadata> metadataById = metadata.stream()
            .filter(item -> item.cinemaSelector() != null && !item.cinemaSelector().isBlank())
            .collect(java.util.stream.Collectors.toMap(
                NearbyRefreshRepository.TheaterSyncMetadata::externalTheaterId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<String, LocalDateTime> latestCollectedAt = repository.findLatestShowtimeCollectedAt(
            CollectorProvider.LOTTE_CINEMA,
            showDate,
            metadataById.keySet()
        );
        List<NearbyRefreshRepository.TheaterSyncMetadata> staleTheaters = new ArrayList<>();
        for (NearbyRefreshRepository.TheaterSyncMetadata item : metadataById.values()) {
            if (!isStale(showDate, latestCollectedAt.get(item.externalTheaterId()))) {
                logger.info("Nearby refresh skipped provider={} theater={} date={} reason=fresh", item.provider(), item.externalTheaterId(), showDate);
                continue;
            }
            if (!acquire(inFlightKey(item.provider(), item.externalTheaterId(), showDate))) {
                logger.info("Nearby refresh skipped provider={} theater={} date={} reason=in_flight", item.provider(), item.externalTheaterId(), showDate);
                continue;
            }
            staleTheaters.add(item);
        }
        if (staleTheaters.isEmpty()) {
            return;
        }
        try {
            for (NearbyRefreshRepository.TheaterSyncMetadata theater : staleTheaters) {
                logger.info(
                    "Nearby refresh discovery starting provider={} theater={} date={}",
                    CollectorProvider.LOTTE_CINEMA,
                    theater.externalTheaterId(),
                    showDate
                );
                PythonCollectorBridge.ProviderDiscoveryPayload discovery = collectorBridge.collectLotteNearbyDiscovery(
                    showDate,
                    theater.cinemaSelector()
                );
                logger.info(
                    "Nearby refresh discovered provider={} theater={} date={} matchedTargets={}",
                    CollectorProvider.LOTTE_CINEMA,
                    theater.externalTheaterId(),
                    showDate,
                    discovery.targets().size()
                );
                for (Map<String, Object> target : discovery.targets()) {
                    persistCollectedBundle(new ShowtimeCollectionRequest(
                        CollectorProvider.LOTTE_CINEMA,
                        showDate,
                        null,
                        null,
                        text(target.get("cinema_selector")),
                        text(target.get("representation_movie_code")),
                        null
                    ));
                }
            }
        } finally {
            releaseForTheaters(CollectorProvider.LOTTE_CINEMA, showDate, staleTheaters.stream().map(NearbyRefreshRepository.TheaterSyncMetadata::externalTheaterId).toList());
        }
    }

    private void refreshMegabox(LocalDate showDate, List<NearbyTheaterTargetResolver.TheaterMapEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<NearbyRefreshRepository.TheaterSyncMetadata> metadata = repository.findTheaterSyncMetadata(
            CollectorProvider.MEGABOX,
            entries.stream().map(NearbyTheaterTargetResolver.TheaterMapEntry::externalTheaterId).toList()
        );
        Map<String, NearbyRefreshRepository.TheaterSyncMetadata> metadataById = metadata.stream()
            .filter(item -> item.areaCode() != null && !item.areaCode().isBlank())
            .collect(java.util.stream.Collectors.toMap(
                NearbyRefreshRepository.TheaterSyncMetadata::externalTheaterId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<String, LocalDateTime> latestCollectedAt = repository.findLatestShowtimeCollectedAt(
            CollectorProvider.MEGABOX,
            showDate,
            metadataById.keySet()
        );
        Map<String, List<NearbyRefreshRepository.TheaterSyncMetadata>> staleAreas = new LinkedHashMap<>();
        for (NearbyRefreshRepository.TheaterSyncMetadata item : metadataById.values()) {
            if (!isStale(showDate, latestCollectedAt.get(item.externalTheaterId()))) {
                logger.info("Nearby refresh skipped provider={} theater={} date={} reason=fresh", item.provider(), item.externalTheaterId(), showDate);
                continue;
            }
            if (!acquire(inFlightAreaKey(item.provider(), item.areaCode(), showDate))) {
                logger.info("Nearby refresh skipped provider={} areaCode={} date={} reason=in_flight", item.provider(), item.areaCode(), showDate);
                continue;
            }
            staleAreas.computeIfAbsent(item.areaCode(), unused -> new ArrayList<>()).add(item);
        }
        if (staleAreas.isEmpty()) {
            return;
        }
        try {
            for (Map.Entry<String, List<NearbyRefreshRepository.TheaterSyncMetadata>> areaEntry : staleAreas.entrySet()) {
                String areaCode = areaEntry.getKey();
                logger.info(
                    "Nearby refresh discovery starting provider={} areaCode={} date={} theaters={}",
                    CollectorProvider.MEGABOX,
                    areaCode,
                    showDate,
                    areaEntry.getValue().size()
                );
                PythonCollectorBridge.ProviderDiscoveryPayload discovery = collectorBridge.collectMegaboxNearbyDiscovery(showDate, areaCode);
                logger.info(
                    "Nearby refresh discovered provider={} areaCode={} date={} matchedTargets={}",
                    CollectorProvider.MEGABOX,
                    areaCode,
                    showDate,
                    discovery.targets().size()
                );
                for (Map<String, Object> target : discovery.targets()) {
                    persistCollectedBundle(new ShowtimeCollectionRequest(
                        CollectorProvider.MEGABOX,
                        showDate,
                        null,
                        text(target.get("movie_no")),
                        null,
                        null,
                        text(target.get("area_code"))
                    ));
                }
            }
        } finally {
            staleAreas.keySet().forEach(areaCode -> release(inFlightAreaKey(CollectorProvider.MEGABOX, areaCode, showDate)));
        }
    }

    private void persistCollectedBundle(ShowtimeCollectionRequest request) {
        Map<String, Object> bundle = collectorBridge.collectShowtimeBundle(request);
        CollectorBundleIngestCommand.IngestResult result = persistenceService.persist(request.provider().name(), bundle, false);
        logger.info(
            "Nearby refresh stored provider={} date={} theaters={} screens={} showtimes={}",
            request.provider(),
            request.playDate(),
            result.theaters(),
            result.screens(),
            result.showtimes()
        );
    }

    private boolean isStale(LocalDate showDate, LocalDateTime lastCollectedAt) {
        if (lastCollectedAt == null) {
            return true;
        }
        LocalDate today = LocalDate.now(clock);
        long dayOffset = ChronoUnit.DAYS.between(today, showDate);
        Duration ttl = dayOffset <= 0
            ? Duration.ofMinutes(properties.getShowtimes().getNearbyRefreshTodayTtlMinutes())
            : dayOffset == 1
                ? Duration.ofMinutes(properties.getShowtimes().getNearbyRefreshNextDayTtlMinutes())
                : Duration.ofMinutes(properties.getShowtimes().getNearbyRefreshFutureTtlMinutes());
        return lastCollectedAt.isBefore(LocalDateTime.now(clock).minus(ttl));
    }

    private boolean acquire(String key) {
        return inFlightKeys.add(key);
    }

    private void release(String key) {
        inFlightKeys.remove(key);
    }

    private void releaseForTheaters(CollectorProvider provider, LocalDate showDate, Collection<String> externalTheaterIds) {
        for (String externalTheaterId : externalTheaterIds) {
            release(inFlightKey(provider, externalTheaterId, showDate));
        }
    }

    private static String inFlightKey(CollectorProvider provider, String externalTheaterId, LocalDate showDate) {
        return provider.name() + ":" + externalTheaterId + ":" + showDate;
    }

    private static String inFlightAreaKey(CollectorProvider provider, String areaCode, LocalDate showDate) {
        return provider.name() + ":AREA:" + areaCode + ":" + showDate;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

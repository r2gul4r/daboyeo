package kr.daboyeo.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.daboyeo.backend.crawler.MovieEventCrawler;
import kr.daboyeo.backend.domain.Category;
import kr.daboyeo.backend.domain.MovieEvent;
import kr.daboyeo.backend.repository.MovieEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieEventService {

    private static final Logger logger = LoggerFactory.getLogger(MovieEventService.class);

    private final List<MovieEventCrawler> crawlers;
    private final MovieEventRepository repository;
    private final long fallbackCacheTtlMillis;
    private volatile CachedFallbackEvents cachedFallbackEvents;

    public MovieEventService(
        List<MovieEventCrawler> crawlers,
        MovieEventRepository repository,
        @Value("${daboyeo.events.fallback-cache-minutes:15}") long fallbackCacheMinutes
    ) {
        this.crawlers = crawlers;
        this.repository = repository;
        this.fallbackCacheTtlMillis = Math.max(1L, fallbackCacheMinutes) * 60_000L;
    }

    @Transactional
    public int crawlAndSaveEvents() {
        List<MovieEvent> eventsToSave = new ArrayList<>();
        Set<String> batchKeys = new HashSet<>();

        for (MovieEventCrawler crawler : crawlers) {
            List<MovieEvent> crawledEvents = crawler.crawlAllEvents();
            for (MovieEvent event : crawledEvents) {
                if (shouldSave(event, batchKeys)) {
                    eventsToSave.add(event);
                }
            }
        }

        repository.saveAll(eventsToSave);
        logger.info("Movie event crawl completed. saved={}", eventsToSave.size());
        return eventsToSave.size();
    }

    public List<MovieEvent> getAllEvents() {
        return queryOrCrawl(repository::findAllOrderByCreatedAtDesc, null, null);
    }

    public List<MovieEvent> getByCategory(Category category) {
        return queryOrCrawl(() -> repository.findByCategory(category), null, category);
    }

    public List<MovieEvent> getBySource(String source) {
        return queryOrCrawl(() -> repository.findBySource(normalizeSource(source)), normalizeSource(source), null);
    }

    public List<MovieEvent> getBySourceAndCategory(String source, Category category) {
        return queryOrCrawl(() -> repository.findBySourceAndCategory(normalizeSource(source), category), normalizeSource(source), category);
    }

    private boolean shouldSave(MovieEvent event, Set<String> batchKeys) {
        if (event == null) {
            return false;
        }
        if (isBlank(event.getCinema()) || isBlank(event.getEventId()) || isBlank(event.getTitle()) || event.getCategory() == null) {
            logger.warn("Skipping invalid movie event source={} cinema={} eventId={} title={}",
                event.getSource(), event.getCinema(), event.getEventId(), event.getTitle());
            return false;
        }

        String key = event.getCinema() + "::" + event.getEventId();
        if (!batchKeys.add(key)) {
            return false;
        }
        return !repository.existsByEventIdAndCinema(event.getEventId(), event.getCinema());
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank.");
        }
        String normalized = source.trim().toUpperCase();
        return switch (normalized) {
            case "LOTTE" -> "LOTTE_CINEMA";
            case "MEGA" -> "MEGABOX";
            default -> normalized;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<MovieEvent> queryOrCrawl(EventQuery query, String normalizedSource, Category category) {
        try {
            return query.execute();
        } catch (DataAccessException exception) {
            logger.warn(
                "Movie event repository query failed. Falling back to live crawl. source={} category={} reason={}",
                normalizedSource,
                category != null ? category.name() : "ALL",
                exception.getClass().getSimpleName()
            );
            return filterCrawledEvents(normalizedSource, category);
        }
    }

    private List<MovieEvent> filterCrawledEvents(String normalizedSource, Category category) {
        List<MovieEvent> allEvents = loadFallbackEvents();
        List<MovieEvent> filteredEvents = new ArrayList<>();
        for (MovieEvent event : allEvents) {
            if (event == null) {
                continue;
            }
            if (normalizedSource != null && !matchesSource(event, normalizedSource)) {
                continue;
            }
            if (category != null && category != event.getCategory()) {
                continue;
            }
            filteredEvents.add(event);
        }
        return filteredEvents;
    }

    private boolean matchesSource(MovieEvent event, String normalizedSource) {
        return normalizedSource.equalsIgnoreCase(event.getSource()) || normalizedSource.equalsIgnoreCase(event.getCinema());
    }

    private List<MovieEvent> loadFallbackEvents() {
        CachedFallbackEvents cache = cachedFallbackEvents;
        long now = System.currentTimeMillis();
        if (cache != null && cache.expiresAtMillis() > now) {
            return cache.events();
        }

        synchronized (this) {
            cache = cachedFallbackEvents;
            if (cache != null && cache.expiresAtMillis() > now) {
                return cache.events();
            }

            List<MovieEvent> crawledEvents = crawlFallbackEvents();
            if (!crawledEvents.isEmpty()) {
                List<MovieEvent> cachedEvents = List.copyOf(crawledEvents);
                cachedFallbackEvents = new CachedFallbackEvents(cachedEvents, now + fallbackCacheTtlMillis);
                return cachedEvents;
            }

            if (cache != null && !cache.events().isEmpty()) {
                logger.warn("Movie event fallback crawl returned no events. Reusing stale cached events.");
                return cache.events();
            }
            return List.of();
        }
    }

    private List<MovieEvent> crawlFallbackEvents() {
        List<MovieEvent> fallbackEvents = new ArrayList<>();
        for (MovieEventCrawler crawler : crawlers) {
            try {
                fallbackEvents.addAll(crawler.crawlAllEvents());
            } catch (Exception exception) {
                logger.warn("Movie event fallback crawl failed for crawler={} reason={}",
                    crawler.getClass().getSimpleName(),
                    exception.getClass().getSimpleName());
            }
        }
        return fallbackEvents;
    }

    @FunctionalInterface
    private interface EventQuery {
        List<MovieEvent> execute();
    }

    private record CachedFallbackEvents(List<MovieEvent> events, long expiresAtMillis) {
    }
}

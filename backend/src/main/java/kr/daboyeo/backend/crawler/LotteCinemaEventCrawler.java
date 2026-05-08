package kr.daboyeo.backend.crawler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.daboyeo.backend.crawler.lotte.LotteCinemaApiClient;
import kr.daboyeo.backend.crawler.lotte.LotteCinemaEventCategory;
import kr.daboyeo.backend.crawler.lotte.LotteCinemaEventParser;
import kr.daboyeo.backend.crawler.lotte.LotteCinemaEventResponse;
import kr.daboyeo.backend.domain.MovieEvent;
import org.springframework.stereotype.Component;

@Component
public class LotteCinemaEventCrawler extends BaseMovieEventCrawler {

    private final LotteCinemaApiClient apiClient;
    private final LotteCinemaEventParser parser;

    public LotteCinemaEventCrawler(LotteCinemaApiClient apiClient, LotteCinemaEventParser parser) {
        super("LOTTE_CINEMA");
        this.apiClient = apiClient;
        this.parser = parser;
    }

    @Override
    protected List<MovieEvent> doCrawl(CrawlingMetrics metrics) {
        Map<String, EventWithPriority> dedupMap = new LinkedHashMap<>();
        for (LotteCinemaEventCategory category : LotteCinemaEventCategory.values()) {
            long startedAt = System.currentTimeMillis();
            try {
                List<MovieEvent> events = crawlCategory(category);
                metrics.addFound(events.size());
                for (MovieEvent event : events) {
                    processDedup(dedupMap, event, category, metrics);
                }
            } catch (Exception exception) {
                metrics.addFailed(1);
                logger.warn("[LOTTE] Event crawl failed for category={} reason={}", category.name(), exception.getMessage());
            } finally {
                metrics.recordCategoryTime(category.name(), System.currentTimeMillis() - startedAt);
            }
        }

        return dedupMap.values().stream()
            .map(EventWithPriority::event)
            .sorted(Comparator.comparing(this::isExpired)
                .thenComparing(MovieEvent::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MovieEvent::getTitle))
            .collect(Collectors.toList());
    }

    private List<MovieEvent> crawlCategory(LotteCinemaEventCategory category) {
        List<MovieEvent> allEvents = new ArrayList<>();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while (allEvents.size() < totalCount) {
            String json = apiClient.fetchEventPage(category.getCode(), pageNo);
            LotteCinemaEventResponse response = parser.parseResponse(json);
            if (response == null || response.getItems().isEmpty()) {
                break;
            }

            totalCount = response.getTotalCount() != null && response.getTotalCount() > 0
                ? response.getTotalCount()
                : Integer.MAX_VALUE;

            List<MovieEvent> pageEvents = parser.toMovieEvents(response, category);
            allEvents.addAll(pageEvents);

            if (pageEvents.size() < 100 || allEvents.size() >= totalCount) {
                break;
            }
            pageNo++;
        }
        return allEvents;
    }

    private void processDedup(
        Map<String, EventWithPriority> dedupMap,
        MovieEvent event,
        LotteCinemaEventCategory category,
        CrawlingMetrics metrics
    ) {
        String eventId = event.getEventId();
        EventWithPriority existing = dedupMap.get(eventId);
        if (existing == null || category.getPriority() < existing.priority()) {
            if (existing != null) {
                metrics.addDeduped(1);
            }
            dedupMap.put(eventId, new EventWithPriority(event, category.getPriority()));
            return;
        }
        metrics.addDeduped(1);
    }

    private boolean isExpired(MovieEvent event) {
        return event.getEndDate() != null && event.getEndDate().isBefore(LocalDate.now());
    }

    private record EventWithPriority(MovieEvent event, int priority) {
    }
}

package kr.daboyeo.backend.crawler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlingMetrics {

    private final String cinemaName;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private final AtomicInteger totalFound = new AtomicInteger();
    private final AtomicInteger dedupedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final Map<String, Long> categoryElapsedMillis = new ConcurrentHashMap<>();

    public CrawlingMetrics(String cinemaName) {
        this.cinemaName = cinemaName;
        this.startedAt = LocalDateTime.now();
    }

    public void markEnd() {
        this.endedAt = LocalDateTime.now();
    }

    public void addFound(int count) {
        totalFound.addAndGet(count);
    }

    public void addDeduped(int count) {
        dedupedCount.addAndGet(count);
    }

    public void addFailed(int count) {
        failedCount.addAndGet(count);
    }

    public void recordCategoryTime(String category, long millis) {
        categoryElapsedMillis.put(category, millis);
    }

    @Override
    public String toString() {
        return String.format(
            "[%s] event crawl summary start=%s end=%s found=%d deduped=%d failed=%d categoryTimes=%s",
            cinemaName,
            startedAt,
            endedAt,
            totalFound.get(),
            dedupedCount.get(),
            failedCount.get(),
            categoryElapsedMillis
        );
    }
}

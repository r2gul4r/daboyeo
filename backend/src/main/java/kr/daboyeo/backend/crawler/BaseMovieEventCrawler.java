package kr.daboyeo.backend.crawler;

import java.util.List;
import kr.daboyeo.backend.domain.MovieEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseMovieEventCrawler implements MovieEventCrawler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final String cinemaName;

    protected BaseMovieEventCrawler(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    @Override
    public final List<MovieEvent> crawlAllEvents() {
        CrawlingMetrics metrics = new CrawlingMetrics(cinemaName);
        logger.info("[{}] Starting event crawling...", cinemaName);
        try {
            List<MovieEvent> events = doCrawl(metrics);
            metrics.markEnd();
            logger.info("{}", metrics);
            return events;
        } catch (Exception exception) {
            metrics.markEnd();
            logger.error("[{}] Fatal event crawling error", cinemaName, exception);
            return List.of();
        }
    }

    protected abstract List<MovieEvent> doCrawl(CrawlingMetrics metrics) throws Exception;
}

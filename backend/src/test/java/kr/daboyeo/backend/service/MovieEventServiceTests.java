package kr.daboyeo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import kr.daboyeo.backend.crawler.MovieEventCrawler;
import kr.daboyeo.backend.domain.Category;
import kr.daboyeo.backend.domain.MovieEvent;
import kr.daboyeo.backend.repository.MovieEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

@ExtendWith(MockitoExtension.class)
class MovieEventServiceTests {

    @Mock
    private MovieEventCrawler movieEventCrawler;

    @Mock
    private MovieEventRepository movieEventRepository;

    @Test
    void fallsBackToLiveCrawlWhenRepositoryQueryFails() {
        MovieEvent event = new MovieEvent(
            "LOTTE_CINEMA",
            "LOTTE_CINEMA",
            "event-1",
            Category.HOT,
            "Summer Event",
            "https://example.com/poster.jpg",
            "https://example.com/event",
            LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 31),
            "D-5"
        );

        given(movieEventRepository.findAllOrderByCreatedAtDesc())
            .willThrow(new BadSqlGrammarException("findAllOrderByCreatedAtDesc", "SELECT * FROM movie_events", null));
        given(movieEventCrawler.crawlAllEvents()).willReturn(List.of(event));

        MovieEventService service = new MovieEventService(List.of(movieEventCrawler), movieEventRepository, 15L);

        List<MovieEvent> result = service.getAllEvents();

        assertThat(result).containsExactly(event);
        verify(movieEventCrawler).crawlAllEvents();
    }

    @Test
    void filtersFallbackEventsBySourceAndCategory() {
        MovieEvent matchingEvent = new MovieEvent(
            "LOTTE_CINEMA",
            "LOTTE_CINEMA",
            "event-1",
            Category.HOT,
            "Summer Event",
            "https://example.com/poster.jpg",
            "https://example.com/event",
            LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 31),
            "D-5"
        );
        MovieEvent nonMatchingEvent = new MovieEvent(
            "MEGABOX",
            "MEGABOX",
            "event-2",
            Category.DISCOUNT,
            "Discount Event",
            "https://example.com/poster-2.jpg",
            "https://example.com/event-2",
            LocalDate.of(2026, 5, 9),
            LocalDate.of(2026, 5, 22),
            "D-2"
        );

        given(movieEventRepository.findBySourceAndCategory("LOTTE_CINEMA", Category.HOT))
            .willThrow(new BadSqlGrammarException("findBySourceAndCategory", "SELECT * FROM movie_events", null));
        given(movieEventCrawler.crawlAllEvents()).willReturn(List.of(matchingEvent, nonMatchingEvent));

        MovieEventService service = new MovieEventService(List.of(movieEventCrawler), movieEventRepository, 15L);

        List<MovieEvent> result = service.getBySourceAndCategory("lotte", Category.HOT);

        assertThat(result).containsExactly(matchingEvent);
    }

    @Test
    void reusesCachedFallbackEventsWithinTtl() {
        MovieEvent cachedEvent = new MovieEvent(
            "LOTTE_CINEMA",
            "LOTTE_CINEMA",
            "event-1",
            Category.HOT,
            "Summer Event",
            "https://example.com/poster.jpg",
            "https://example.com/event",
            LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 31),
            "D-5"
        );

        given(movieEventRepository.findAllOrderByCreatedAtDesc())
            .willThrow(new BadSqlGrammarException("findAllOrderByCreatedAtDesc", "SELECT * FROM movie_events", null));
        given(movieEventCrawler.crawlAllEvents()).willReturn(List.of(cachedEvent));

        MovieEventService service = new MovieEventService(List.of(movieEventCrawler), movieEventRepository, 15L);

        assertThat(service.getAllEvents()).containsExactly(cachedEvent);
        assertThat(service.getAllEvents()).containsExactly(cachedEvent);
        verify(movieEventCrawler, times(1)).crawlAllEvents();
    }
}

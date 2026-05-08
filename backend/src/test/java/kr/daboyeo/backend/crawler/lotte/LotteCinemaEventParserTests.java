package kr.daboyeo.backend.crawler.lotte;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import kr.daboyeo.backend.domain.MovieEvent;
import org.junit.jupiter.api.Test;

class LotteCinemaEventParserTests {

    private final LotteCinemaEventParser parser = new LotteCinemaEventParser(new ObjectMapper());

    @Test
    void mapsResponseItemsIntoMovieEvents() {
        String json = """
            {
              "Items": [
                {
                  "EventID": "evt-001",
                  "EventName": "<b>Summer Event</b>",
                  "ImageUrl": "/images/event.jpg",
                  "ProgressStartDate": "2026.05.08",
                  "ProgressEndDate": "2026.05.31",
                  "RemainsDayCount": 5
                }
              ],
              "TotalCount": 1,
              "IsOK": "true"
            }
            """;

        LotteCinemaEventResponse response = parser.parseResponse(json);
        List<MovieEvent> events = parser.toMovieEvents(response, LotteCinemaEventCategory.HOT);

        assertThat(events).hasSize(1);
        MovieEvent event = events.get(0);
        assertThat(event.getCinema()).isEqualTo("LOTTE_CINEMA");
        assertThat(event.getEventId()).isEqualTo("evt-001");
        assertThat(event.getCategory().name()).isEqualTo("HOT");
        assertThat(event.getTitle()).isEqualTo("Summer Event");
        assertThat(event.getImageUrl()).isEqualTo("https://www.lottecinema.co.kr/images/event.jpg");
        assertThat(event.getEventUrl()).contains("evt-001");
        assertThat(event.getDDay()).isEqualTo("오늘오픈");
    }
}

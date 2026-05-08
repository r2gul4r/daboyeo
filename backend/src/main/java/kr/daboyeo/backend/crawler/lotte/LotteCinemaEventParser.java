package kr.daboyeo.backend.crawler.lotte;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import kr.daboyeo.backend.domain.MovieEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LotteCinemaEventParser {

    private static final Logger logger = LoggerFactory.getLogger(LotteCinemaEventParser.class);

    private final ObjectMapper objectMapper;

    public LotteCinemaEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LotteCinemaEventResponse parseResponse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LotteCinemaEventResponse.class);
        } catch (Exception exception) {
            logger.warn("[LOTTE] Failed to parse event response: {}", exception.getMessage());
            return null;
        }
    }

    public List<MovieEvent> toMovieEvents(LotteCinemaEventResponse response, LotteCinemaEventCategory category) {
        if (response == null || response.getItems().isEmpty()) {
            return List.of();
        }

        List<MovieEvent> events = new ArrayList<>();
        for (LotteCinemaEventResponse.Item item : response.getItems()) {
            MovieEvent event = mapToDomain(item, category);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private MovieEvent mapToDomain(LotteCinemaEventResponse.Item item, LotteCinemaEventCategory category) {
        String title = LotteCinemaEventUtils.cleanHtml(
            item.getEventName() != null && !item.getEventName().isBlank() ? item.getEventName() : item.getImageAlt()
        );
        if (title.isBlank()) {
            return null;
        }

        String eventId = item.getEventId() != null && !item.getEventId().isBlank() ? item.getEventId() : title;
        return new MovieEvent(
            "LOTTE_CINEMA",
            "LOTTE_CINEMA",
            eventId,
            category.getDomainCategory(),
            title,
            LotteCinemaEventUtils.normalizeImageUrl(item.getImageUrl()),
            LotteCinemaEventUtils.buildEventUrl(eventId),
            LotteCinemaEventUtils.parseDate(item.getProgressStartDate()),
            LotteCinemaEventUtils.parseDate(item.getProgressEndDate()),
            LotteCinemaEventUtils.buildDDay(item)
        );
    }
}

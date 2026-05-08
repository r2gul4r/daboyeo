package kr.daboyeo.backend.api;

import java.util.List;
import java.util.Map;
import kr.daboyeo.backend.domain.Category;
import kr.daboyeo.backend.domain.MovieEvent;
import kr.daboyeo.backend.service.MovieEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class MovieEventController {

    private final MovieEventService movieEventService;

    public MovieEventController(MovieEventService movieEventService) {
        this.movieEventService = movieEventService;
    }

    @GetMapping
    public List<MovieEvent> getEvents(
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String category
    ) {
        Category resolvedCategory = Category.fromQueryValue(category);
        if (source != null && !source.isBlank() && resolvedCategory != null) {
            return movieEventService.getBySourceAndCategory(source, resolvedCategory);
        }
        if (source != null && !source.isBlank()) {
            return movieEventService.getBySource(source);
        }
        if (resolvedCategory != null) {
            return movieEventService.getByCategory(resolvedCategory);
        }
        return movieEventService.getAllEvents();
    }

    @PostMapping("/crawl")
    public Map<String, Object> crawlEvents() {
        int savedCount = movieEventService.crawlAndSaveEvents();
        return Map.of(
            "status", "ok",
            "savedCount", savedCount
        );
    }
}

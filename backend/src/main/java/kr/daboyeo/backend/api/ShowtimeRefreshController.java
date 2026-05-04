package kr.daboyeo.backend.api;

import kr.daboyeo.backend.sync.showtime.EntryShowtimeRefreshService;
import kr.daboyeo.backend.sync.showtime.EntryShowtimeRefreshService.EntryShowtimeRefreshRequest;
import kr.daboyeo.backend.sync.showtime.EntryShowtimeRefreshService.EntryShowtimeRefreshResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShowtimeRefreshController {

    private final EntryShowtimeRefreshService refreshService;

    public ShowtimeRefreshController(EntryShowtimeRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/showtimes/refresh")
    public EntryShowtimeRefreshResponse refreshShowtimes(@RequestBody(required = false) EntryShowtimeRefreshRequest request) {
        return refreshService.requestRefresh(request);
    }
}

package kr.daboyeo.backend.sync;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeSyncScheduler {

    private final ShowtimeSyncService service;

    public ShowtimeSyncScheduler(ShowtimeSyncService service) {
        this.service = service;
    }

    @Scheduled(cron = "${daboyeo.sync.showtimes.cron:0 0 3 * * *}", zone = "${daboyeo.sync.timezone:Asia/Seoul}")
    public void run() {
        service.syncDailyShowtimes();
    }
}

package kr.daboyeo.backend.config;

import kr.daboyeo.backend.service.MovieEventService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ScheduledTaskConfig {

    private final MovieEventService movieEventService;

    public ScheduledTaskConfig(MovieEventService movieEventService) {
        this.movieEventService = movieEventService;
    }

    @Scheduled(fixedRate = 3 * 60 * 60 * 1000) // 3시간마다
    public void crawlMovieEvents() {
        movieEventService.crawlAndSaveEvents();
    }
}
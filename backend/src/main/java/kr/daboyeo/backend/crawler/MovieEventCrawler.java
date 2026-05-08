package kr.daboyeo.backend.crawler;

import java.util.List;
import kr.daboyeo.backend.domain.MovieEvent;

public interface MovieEventCrawler {

    List<MovieEvent> crawlAllEvents();
}

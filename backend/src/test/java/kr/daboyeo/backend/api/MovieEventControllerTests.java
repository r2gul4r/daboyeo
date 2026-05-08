package kr.daboyeo.backend.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import kr.daboyeo.backend.domain.Category;
import kr.daboyeo.backend.domain.MovieEvent;
import kr.daboyeo.backend.service.MovieEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MovieEventController.class)
class MovieEventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieEventService movieEventService;

    @Test
    void filtersBySourceAndCategory() throws Exception {
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
        given(movieEventService.getBySourceAndCategory(eq("lotte"), eq(Category.HOT))).willReturn(List.of(event));

        mockMvc.perform(get("/api/events")
                .param("source", "lotte")
                .param("category", "hot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventId").value("event-1"))
            .andExpect(jsonPath("$[0].category").value("HOT"));
    }

    @Test
    void returnsSavedCountAfterCrawl() throws Exception {
        given(movieEventService.crawlAndSaveEvents()).willReturn(3);

        mockMvc.perform(post("/api/events/crawl"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.savedCount").value(3));
    }
}

package kr.daboyeo.backend.repository.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class ShowtimeRecommendationRepositoryTests {

    @Test
    void findUpcomingCandidatesFiltersByUsableStartTime() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShowtimeRecommendationRepository repository = new ShowtimeRecommendationRepository(jdbcTemplate);
        LocalDateTime cutoff = LocalDateTime.of(2026, 4, 17, 16, 10);

        repository.findUpcomingCandidates(10, cutoff);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
            sql.capture(),
            org.mockito.ArgumentMatchers.<ResultSetExtractor<List<ShowtimeCandidate>>>any(),
            eq(Timestamp.valueOf(cutoff)),
            eq(10)
        );
        assertThat(sql.getValue()).contains("s.starts_at >= ?");
        assertThat(sql.getValue()).doesNotContain("s.show_date >= CURRENT_DATE()");
    }

    @Test
    void countStoredShowtimesReturnsZeroWhenDatabaseCountIsNull() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShowtimeRecommendationRepository repository = new ShowtimeRecommendationRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class)).thenReturn(null);

        assertThat(repository.countStoredShowtimes()).isZero();
    }
}

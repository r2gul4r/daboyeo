package kr.daboyeo.backend.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import org.junit.jupiter.api.Test;

class RecommendationScorerTimeTests {

    private final RecommendationScorer scorer = new RecommendationScorer();

    @Test
    void childAudiencePenalizesLateNightShowtime() {
        TagProfile profile = new TagProfile();
        profile.setAudience("child");

        int earlyScore = scorer.scoreOne(profile, candidateAt(18)).orElseThrow().score();
        int lateScore = scorer.scoreOne(profile, candidateAt(21)).orElseThrow().score();

        assertThat(lateScore).isLessThan(earlyScore);
    }

    @Test
    void excitingMoodGetsEveningBonus() {
        TagProfile profile = new TagProfile();
        profile.setMood("exciting");

        int afternoonScore = scorer.scoreOne(profile, candidateAt(14)).orElseThrow().score();
        int eveningScore = scorer.scoreOne(profile, candidateAt(19)).orElseThrow().score();

        assertThat(eveningScore).isGreaterThan(afternoonScore);
    }

    private ShowtimeCandidate candidateAt(int hour) {
        LocalDateTime startsAt = LocalDateTime.of(2026, 4, 17, hour, 0);
        return new ShowtimeCandidate(
            1L,
            (long) hour,
            "Test Movie",
            "MEGABOX",
            "movie-1",
            "Test Theater",
            "Seoul",
            "1",
            "2D",
            "2D",
            LocalDate.from(startsAt),
            startsAt,
            startsAt.plusHours(2),
            80,
            100,
            12_000,
            "KRW",
            "https://example.test/booking",
            "https://example.test/poster.jpg",
            "12",
            100,
            Set.of()
        );
    }
}

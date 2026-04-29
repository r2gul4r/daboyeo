package kr.daboyeo.backend.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import org.junit.jupiter.api.Test;

class RecommendationScorerTests {

    private final RecommendationScorer scorer = new RecommendationScorer();

    @Test
    void childAudienceBlocksUnsafeAgeRating() {
        TagProfile profile = new TagProfile();
        profile.setAudience("child");

        assertThat(scorer.scoreOne(profile, candidate("15세", 100, Set.of("genre:animation")))).isEmpty();
    }

    @Test
    void childAudienceBlocksViolenceTags() {
        TagProfile profile = new TagProfile();
        profile.setAudience("child");

        assertThat(scorer.scoreOne(profile, candidate("12세", 100, Set.of("content:violence")))).isEmpty();
    }

    @Test
    void avoidTooLongAppliesPenaltyInsteadOfDroppingCandidate() {
        TagProfile profile = new TagProfile();
        profile.addAvoid(Set.of("too_long"));

        var scored = scorer.scoreOne(profile, candidate("12세", 165, Set.of("mood:immersive")));

        assertThat(scored).isPresent();
        assertThat(scored.orElseThrow().penalties()).contains("too_long");
    }

    @Test
    void matchingTagsIncreaseScore() {
        TagProfile profile = new TagProfile();
        profile.addWeight("genre:action", 4);
        profile.addWeight("mood:exciting", 3);

        var scored = scorer.scoreOne(profile, candidate("12세", 105, Set.of("genre:action", "mood:exciting")));

        assertThat(scored).isPresent();
        assertThat(scored.orElseThrow().score()).isGreaterThan(70);
    }

    private ShowtimeCandidate candidate(String ageRating, Integer runtimeMinutes, Set<String> tags) {
        return new ShowtimeCandidate(
            1L,
            10L,
            "테스트 영화",
            "CGV",
            "movie-1",
            "테스트 극장",
            "서울",
            "1관",
            "2D",
            "2D",
            LocalDate.now(),
            LocalDateTime.now().plusHours(2),
            LocalDateTime.now().plusHours(4),
            40,
            100,
            12_000,
            "KRW",
            "https://example.test/booking",
            "https://example.test/poster.jpg",
            ageRating,
            runtimeMinutes,
            tags
        );
    }
}

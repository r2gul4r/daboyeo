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

    @Test
    void providerDerivedAnimationTagsBoostChildAndFamilyCandidates() {
        TagProfile profile = new TagProfile();
        profile.setAudience("child");
        profile.addWeight("audience:child", 4);
        profile.addWeight("genre:animation", 4);
        profile.addWeight("mood:light", 3);

        var scored = scorer.scoreOne(profile, candidate(
            "슈퍼 마리오 갤럭시",
            "전체",
            100,
            Set.of("genre:일반콘텐트", "age_rating:all")
        ));

        assertThat(scored).isPresent();
        assertThat(scored.orElseThrow().matchedTags()).contains("audience:child", "genre:animation", "mood:light");
        assertThat(scored.orElseThrow().score()).isGreaterThan(80);
    }

    @Test
    void providerDerivedHorrorTagsBlockChildAudience() {
        TagProfile profile = new TagProfile();
        profile.setAudience("child");

        var scored = scorer.scoreOne(profile, candidate(
            "살목지",
            "12",
            100,
            Set.of("genre:일반콘텐트", "age_rating:12")
        ));

        assertThat(scored).isEmpty();
    }

    @Test
    void providerDerivedTenseTagsFavorTenseProfiles() {
        TagProfile profile = new TagProfile();
        profile.setAudience("friends");
        profile.setMood("tense");
        profile.addWeight("audience:friends", 4);
        profile.addWeight("mood:tense", 4);
        profile.addWeight("genre:thriller", 2);

        var horror = scorer.scoreOne(profile, candidate("살목지", "15", 100, Set.of("genre:일반콘텐트", "age_rating:15")));
        var animation = scorer.scoreOne(profile, candidate("슈퍼 마리오 갤럭시", "전체", 100, Set.of("genre:일반콘텐트", "age_rating:all")));

        assertThat(horror).isPresent();
        assertThat(animation).isPresent();
        assertThat(horror.orElseThrow().score()).isGreaterThan(animation.orElseThrow().score());
        assertThat(horror.orElseThrow().matchedTags()).contains("audience:friends", "mood:tense", "genre:thriller");
    }

    private ShowtimeCandidate candidate(String ageRating, Integer runtimeMinutes, Set<String> tags) {
        return candidate("테스트 영화", ageRating, runtimeMinutes, tags);
    }

    private ShowtimeCandidate candidate(String title, String ageRating, Integer runtimeMinutes, Set<String> tags) {
        return new ShowtimeCandidate(
            1L,
            10L,
            title,
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

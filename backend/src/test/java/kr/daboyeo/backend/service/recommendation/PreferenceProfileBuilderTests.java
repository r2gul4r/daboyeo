package kr.daboyeo.backend.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.PosterChoices;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationSurvey;
import org.junit.jupiter.api.Test;

class PreferenceProfileBuilderTests {

    private final PosterSeedService posterSeedService = new PosterSeedService(new ObjectMapper());
    private final PreferenceProfileBuilder builder = new PreferenceProfileBuilder(posterSeedService);

    @Test
    void buildsWeightsFromSurveyPosterAndStoredFeedback() {
        var profile = builder.build(
            new RecommendationSurvey("friends", "exciting", List.of("too_long")),
            new PosterChoices(
                List.of("avatar", "avengers_endgame", "spiderman_no_way_home"),
                List.of()
            ),
            Map.of("genre:action", 2)
        );

        assertThat(profile.audience()).isEqualTo("friends");
        assertThat(profile.mood()).isEqualTo("exciting");
        assertThat(profile.avoids("too_long")).isTrue();
        assertThat(profile.weight("genre:action")).isEqualTo(14);
        assertThat(profile.weight("mood:visual")).isEqualTo(5);
    }

    @Test
    void requiresAtLeastThreeLikedPosters() {
        assertThatThrownBy(() -> builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(List.of("avatar", "joker"), List.of()),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 3");
    }

    @Test
    void rejectsMoreThanFiveLikedPosters() {
        assertThatThrownBy(() -> builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(
                List.of("avatar", "avengers_endgame", "spiderman_no_way_home", "top_gun_maverick", "super_mario_bros", "barbie"),
                List.of()
            ),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at most 5");
    }

    @Test
    void ignoresDislikedPostersForPosterDiagnosis() {
        var profile = builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(
                List.of("avatar", "avengers_endgame", "spiderman_no_way_home"),
                List.of("joker", "parasite", "titanic")
            ),
            Map.of()
        );

        assertThat(profile.weight("mood:dark")).isZero();
    }
}

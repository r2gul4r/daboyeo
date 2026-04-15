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
                List.of("avatar", "avengers_endgame", "spiderman_no_way_home", "top_gun_maverick", "super_mario_bros"),
                List.of("joker", "parasite", "titanic")
            ),
            Map.of("genre:action", 2)
        );

        assertThat(profile.audience()).isEqualTo("friends");
        assertThat(profile.mood()).isEqualTo("exciting");
        assertThat(profile.avoids("too_long")).isTrue();
        assertThat(profile.weight("genre:action")).isGreaterThan(2);
        assertThat(profile.weight("mood:dark")).isLessThan(0);
    }

    @Test
    void requiresFiveLikedAndThreeDislikedPosters() {
        assertThatThrownBy(() -> builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(List.of("avatar"), List.of("joker")),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("끌리는 포스터 5개");
    }
}

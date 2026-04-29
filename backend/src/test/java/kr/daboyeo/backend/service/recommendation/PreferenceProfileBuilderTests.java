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
                List.of("20129370", "20182530", "20150976"),
                List.of()
            ),
            Map.of("genre:action", 2)
        );

        assertThat(profile.audience()).isEqualTo("friends");
        assertThat(profile.mood()).isEqualTo("exciting");
        assertThat(profile.avoids("too_long")).isTrue();
        assertThat(profile.weight("genre:action")).isEqualTo(4);
        assertThat(profile.weight("genre:popular")).isEqualTo(15);
        assertThat(profile.weight("mood:visual")).isEqualTo(15);
    }

    @Test
    void requiresAtLeastThreeLikedPosters() {
        assertThatThrownBy(() -> builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(List.of("20129370", "20182530"), List.of()),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("끌리는 포스터를 3개 이상");
    }

    @Test
    void rejectsMoreThanFiveLikedPosters() {
        assertThatThrownBy(() -> builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(
                List.of("20129370", "20182530", "20150976", "20137048", "20184889", "20197803"),
                List.of()
            ),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("끌리는 포스터는 5개까지만");
    }

    @Test
    void ignoresDislikedPostersForPosterDiagnosis() {
        var profile = builder.build(
            new RecommendationSurvey("friends", "exciting", List.of()),
            new PosterChoices(
                List.of("20129370", "20182530", "20150976"),
                List.of("20137048", "20184889", "20197803")
            ),
            Map.of()
        );

        assertThat(profile.weight("mood:dark")).isZero();
    }
}

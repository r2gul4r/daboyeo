package kr.daboyeo.backend.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.daboyeo.backend.config.RecommendationProperties;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.PosterChoices;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationProfile;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationRequest;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationResponse;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationSurvey;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import kr.daboyeo.backend.repository.recommendation.RecommendationProfileRepository;
import kr.daboyeo.backend.repository.recommendation.ShowtimeRecommendationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecommendationServiceCandidateFilterTests {

    private final PosterSeedService posterSeedService = mock(PosterSeedService.class);
    private final PreferenceProfileBuilder profileBuilder = mock(PreferenceProfileBuilder.class);
    private final RecommendationScorer scorer = mock(RecommendationScorer.class);
    private final LocalModelRecommendationClient localModelClient = mock(LocalModelRecommendationClient.class);
    private final RecommendationProfileRepository profileRepository = mock(RecommendationProfileRepository.class);
    private final ShowtimeRecommendationRepository showtimeRepository = mock(ShowtimeRecommendationRepository.class);

    @Test
    void noShowtimeDataStatusWhenDatabaseIsEmpty() {
        RecommendationService service = service(20);
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class))).thenReturn(List.of());
        when(showtimeRepository.countStoredShowtimes()).thenReturn(0);

        RecommendationResponse response = service.recommend(request());

        assertThat(response.status()).isEqualTo("no_showtime_data");
        assertThat(response.recommendations()).isEmpty();
        verify(profileRepository).saveRun(
            eq(response.runId()),
            eq("anon_test"),
            eq("fast"),
            eq("gemma-4-e2b-it"),
            any(),
            eq(List.of()),
            eq(null),
            eq(response),
            any(Long.class)
        );
    }

    @Test
    void noUsableShowtimesStatusWhenStoredDataIsOnlyPastOrTooSoon() {
        RecommendationService service = service(20);
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class))).thenReturn(List.of());
        when(showtimeRepository.countStoredShowtimes()).thenReturn(7);

        RecommendationResponse response = service.recommend(request());

        assertThat(response.status()).isEqualTo("no_usable_showtimes");
        assertThat(response.message()).contains("수집된 상영 정보");
    }

    @Test
    void passesConfiguredStartBufferToCandidateQuery() {
        RecommendationService service = service(30);
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class))).thenReturn(List.of());
        when(showtimeRepository.countStoredShowtimes()).thenReturn(1);
        LocalDateTime lowerBound = LocalDateTime.now().plusMinutes(29);

        service.recommend(request());

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(showtimeRepository).findUpcomingCandidates(anyInt(), cutoff.capture());
        assertThat(cutoff.getValue()).isAfterOrEqualTo(lowerBound);
    }

    private RecommendationService service(int minStartBufferMinutes) {
        when(profileRepository.findProfile("anon_test"))
            .thenReturn(Optional.of(new RecommendationProfile("anon_test", Map.of())));
        when(profileBuilder.build(any(), any(), any())).thenReturn(new TagProfile());
        RecommendationProperties properties = new RecommendationProperties(
            null,
            null,
            null,
            minStartBufferMinutes,
            List.of("http://localhost:5173")
        );
        return new RecommendationService(
            properties,
            posterSeedService,
            profileBuilder,
            scorer,
            localModelClient,
            profileRepository,
            showtimeRepository
        );
    }

    private RecommendationRequest request() {
        return new RecommendationRequest(
            "anon_test",
            "fast",
            new RecommendationSurvey("friends", "light", List.of("too_long")),
            new PosterChoices(List.of("barbie", "aladdin_2019", "inside_out_2"), List.of())
        );
    }
}

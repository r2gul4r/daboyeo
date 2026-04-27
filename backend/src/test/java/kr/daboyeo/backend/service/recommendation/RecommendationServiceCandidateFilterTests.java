package kr.daboyeo.backend.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kr.daboyeo.backend.config.RecommendationProperties;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.PosterChoices;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationProfile;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationRequest;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationResponse;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationSurvey;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ScoredCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.SearchFilters;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
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
        assertThat(response.message()).isNotBlank();
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

    @Test
    void passesConfiguredFastAiCandidateLimitToLocalModel() {
        RecommendationService service = service(20, 3, 5);
        ShowtimeCandidate first = candidate(1, "First");
        ShowtimeCandidate second = candidate(2, "Second");
        ShowtimeCandidate third = candidate(3, "Third");
        ShowtimeCandidate fourth = candidate(4, "Fourth");
        List<ScoredCandidate> scored = List.of(
            scored(first, 90),
            scored(second, 80),
            scored(third, 70),
            scored(fourth, 60)
        );
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class)))
            .thenReturn(List.of(first, second, third, fourth));
        when(scorer.score(any(TagProfile.class), any(), any())).thenReturn(scored);
        when(localModelClient.rankAndExplain(any(), any(), any())).thenReturn(Optional.empty());

        service.recommend(request());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoredCandidate>> aiCandidates = ArgumentCaptor.forClass(List.class);
        verify(localModelClient).rankAndExplain(eq(RecommendationMode.FAST), any(TagProfile.class), aiCandidates.capture());
        assertThat(aiCandidates.getValue()).hasSize(3);
    }

    @Test
    void passesConfiguredPreciseAiCandidateLimitToLocalModel() {
        RecommendationService service = service(20, 5, 5);
        ShowtimeCandidate first = candidate(1, "First");
        ShowtimeCandidate second = candidate(2, "Second");
        ShowtimeCandidate third = candidate(3, "Third");
        ShowtimeCandidate fourth = candidate(4, "Fourth");
        ShowtimeCandidate fifth = candidate(5, "Fifth");
        ShowtimeCandidate sixth = candidate(6, "Sixth");
        List<ScoredCandidate> scored = List.of(
            scored(first, 90),
            scored(second, 80),
            scored(third, 70),
            scored(fourth, 60),
            scored(fifth, 50),
            scored(sixth, 40)
        );
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class)))
            .thenReturn(List.of(first, second, third, fourth, fifth, sixth));
        when(scorer.score(any(TagProfile.class), any(), any())).thenReturn(scored);
        when(localModelClient.rankAndExplain(any(), any(), any())).thenReturn(Optional.empty());

        service.recommend(request("precise"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoredCandidate>> aiCandidates = ArgumentCaptor.forClass(List.class);
        verify(localModelClient).rankAndExplain(eq(RecommendationMode.PRECISE), any(TagProfile.class), aiCandidates.capture());
        assertThat(aiCandidates.getValue()).hasSize(5);
    }

    @Test
    void passesSearchFiltersToCandidateQueryWhenPresent() {
        RecommendationService service = service(20);
        SearchFilters filters = new SearchFilters("Gangnam", LocalDate.of(2026, 4, 17), "night", 2);
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class), eq(filters))).thenReturn(List.of());

        service.recommend(request("fast", filters));

        verify(showtimeRepository).findUpcomingCandidates(anyInt(), any(LocalDateTime.class), eq(filters));
    }

    @Test
    void noFilteredCandidatesStatusWhenSearchFiltersReturnNoResults() {
        RecommendationService service = service(20);
        SearchFilters filters = new SearchFilters("Gangnam", LocalDate.of(2026, 4, 17), "night", 2);
        when(showtimeRepository.findUpcomingCandidates(anyInt(), any(LocalDateTime.class), eq(filters))).thenReturn(List.of());

        RecommendationResponse response = service.recommend(request("fast", filters));

        assertThat(response.status()).isEqualTo("no_filtered_candidates");
        assertThat(response.message()).contains("지역", "날짜", "시간대", "인원수");
    }

    private RecommendationService service(int minStartBufferMinutes) {
        return service(minStartBufferMinutes, 5, 5);
    }

    private RecommendationService service(int minStartBufferMinutes, int fastAiCandidateLimit, int preciseAiCandidateLimit) {
        when(profileRepository.findProfile("anon_test"))
            .thenReturn(Optional.of(new RecommendationProfile("anon_test", Map.of())));
        when(profileBuilder.build(any(), any(), any())).thenReturn(new TagProfile());
        RecommendationProperties properties = new RecommendationProperties(
            null,
            null,
            null,
            minStartBufferMinutes,
            fastAiCandidateLimit,
            preciseAiCandidateLimit,
            280,
            320,
            72,
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

    private ScoredCandidate scored(ShowtimeCandidate candidate, int score) {
        return new ScoredCandidate(candidate, score, List.of("mood:light"), List.of());
    }

    private ShowtimeCandidate candidate(long id, String title) {
        return new ShowtimeCandidate(
            id,
            100L + id,
            title,
            "lotte",
            "external-" + id,
            "Test Theater",
            "Seoul",
            "1관",
            "",
            "",
            LocalDate.now(),
            LocalDateTime.now().plusHours(2),
            null,
            50,
            100,
            12_000,
            "KRW",
            "",
            "",
            "12",
            120,
            Set.of("mood:light")
        );
    }

    private RecommendationRequest request() {
        return request("fast");
    }

    private RecommendationRequest request(String mode) {
        return request(mode, null);
    }

    private RecommendationRequest request(String mode, SearchFilters filters) {
        return new RecommendationRequest(
            "anon_test",
            mode,
            new RecommendationSurvey("friends", "light", List.of("too_long")),
            new PosterChoices(List.of("barbie", "aladdin_2019", "inside_out_2"), List.of()),
            filters
        );
    }
}

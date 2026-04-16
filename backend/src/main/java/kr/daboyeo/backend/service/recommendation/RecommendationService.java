package kr.daboyeo.backend.service.recommendation;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.daboyeo.backend.config.RecommendationProperties;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiPick;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.FeedbackAction;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.FeedbackRequest;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.FeedbackResponse;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.PosterSeedMovie;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationItem;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationProfile;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationRequest;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationResponse;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ScoredCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.SessionResponse;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import kr.daboyeo.backend.repository.recommendation.RecommendationProfileRepository;
import kr.daboyeo.backend.repository.recommendation.ShowtimeRecommendationRepository;
import org.springframework.stereotype.Service;

import static kr.daboyeo.backend.domain.recommendation.RecommendationModels.newAnonymousId;
import static kr.daboyeo.backend.domain.recommendation.RecommendationModels.newRunId;
import static kr.daboyeo.backend.domain.recommendation.RecommendationModels.normalizeAnonymousId;

@Service
public class RecommendationService {

    private static final int CANDIDATE_LIMIT = 240;
    private static final int AI_CANDIDATE_LIMIT = 8;
    private static final int RESULT_LIMIT = 3;

    private final RecommendationProperties properties;
    private final PosterSeedService posterSeedService;
    private final PreferenceProfileBuilder preferenceProfileBuilder;
    private final RecommendationScorer scorer;
    private final LocalModelRecommendationClient localModelClient;
    private final RecommendationProfileRepository profileRepository;
    private final ShowtimeRecommendationRepository showtimeRepository;

    public RecommendationService(
        RecommendationProperties properties,
        PosterSeedService posterSeedService,
        PreferenceProfileBuilder preferenceProfileBuilder,
        RecommendationScorer scorer,
        LocalModelRecommendationClient localModelClient,
        RecommendationProfileRepository profileRepository,
        ShowtimeRecommendationRepository showtimeRepository
    ) {
        this.properties = properties;
        this.posterSeedService = posterSeedService;
        this.preferenceProfileBuilder = preferenceProfileBuilder;
        this.scorer = scorer;
        this.localModelClient = localModelClient;
        this.profileRepository = profileRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public SessionResponse ensureSession(String requestedAnonymousId) {
        String anonymousId = normalizeAnonymousId(requestedAnonymousId);
        if (anonymousId.isBlank()) {
            anonymousId = newAnonymousId();
        }
        profileRepository.ensureProfile(anonymousId);
        return new SessionResponse(anonymousId);
    }

    public void resetSession(String anonymousId) {
        String normalized = normalizeAnonymousId(anonymousId);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("anonymousId가 필요해.");
        }
        profileRepository.deleteProfileData(normalized);
    }

    public List<PosterSeedMovie> posterSeed(int limit) {
        return posterSeedService.randomSeed(limit <= 0 ? 10 : limit);
    }

    public RecommendationResponse recommend(RecommendationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("추천 요청이 필요해.");
        }
        Instant startedAt = Instant.now();
        RecommendationMode mode = RecommendationMode.from(request.mode());
        String anonymousId = normalizeAnonymousId(request.anonymousId());
        if (anonymousId.isBlank()) {
            anonymousId = ensureSession(null).anonymousId();
        } else {
            profileRepository.ensureProfile(anonymousId);
        }

        final String resolvedAnonymousId = anonymousId;
        RecommendationProfile storedProfile = profileRepository.findProfile(resolvedAnonymousId)
            .orElseGet(() -> new RecommendationProfile(resolvedAnonymousId, Map.of()));
        TagProfile tagProfile = preferenceProfileBuilder.build(
            request.survey(),
            request.posterChoices(),
            storedProfile.tagWeights()
        );
        RecommendationRequest normalizedRequest = new RecommendationRequest(
            resolvedAnonymousId,
            mode.wireValue(),
            request.survey(),
            request.posterChoices()
        );
        profileRepository.upsertSurvey(resolvedAnonymousId, normalizedRequest);

        String runId = newRunId();
        List<ShowtimeCandidate> candidates = showtimeRepository.findUpcomingCandidates(CANDIDATE_LIMIT);
        List<ScoredCandidate> scored = scorer.score(tagProfile, candidates);
        String modelName = properties.modelFor(mode);

        if (scored.isEmpty()) {
            RecommendationResponse response = new RecommendationResponse(
                runId,
                mode.wireValue(),
                modelName,
                "no_candidates",
                "현재 조건으로 추천할 수 있는 상영 후보가 없어. 수집 데이터나 날짜 조건을 확인해줘.",
                List.of()
            );
            profileRepository.saveRun(
                runId,
                resolvedAnonymousId,
                mode.wireValue(),
                modelName,
                normalizedRequest,
                List.of(),
                null,
                response,
                elapsedMs(startedAt)
            );
            return response;
        }

        List<ScoredCandidate> aiCandidates = scored.stream().limit(AI_CANDIDATE_LIMIT).toList();
        var aiResult = localModelClient.rankAndExplain(mode, tagProfile, aiCandidates);
        List<RecommendationItem> items = aiResult
            .map(result -> itemsFromAi(aiCandidates, result.picks()))
            .filter(list -> !list.isEmpty())
            .orElseGet(() -> fallbackItems(aiCandidates));

        RecommendationResponse response = new RecommendationResponse(
            runId,
            mode.wireValue(),
            aiResult.map(result -> result.modelName()).orElse(modelName == null || modelName.isBlank() ? "not-configured" : modelName),
            aiResult.isPresent() ? "ok" : "fallback",
            aiResult.isPresent()
                ? "로컬 Gemma가 후보를 재정렬하고 이유를 작성했어."
                : "AI 응답을 쓰지 못해서 코드 점수 기반으로 추천했어.",
            items
        );
        profileRepository.saveRun(
            runId,
            resolvedAnonymousId,
            mode.wireValue(),
            response.model(),
            normalizedRequest,
            aiCandidates,
            aiResult.map(result -> result.rawJson()).orElse(null),
            response,
            elapsedMs(startedAt)
        );
        return response;
    }

    public FeedbackResponse feedback(String runId, FeedbackRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("피드백 요청이 필요해.");
        }
        String anonymousId = normalizeAnonymousId(request.anonymousId());
        if (anonymousId.isBlank()) {
            throw new IllegalArgumentException("anonymousId가 필요해.");
        }
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId가 필요해.");
        }
        FeedbackAction action = FeedbackAction.from(request.action());
        Map<String, Integer> delta = showtimeRepository.findByShowtimeId(request.showtimeId())
            .map(candidate -> feedbackDelta(candidate, action))
            .orElseGet(Map::of);
        RecommendationProfile profile = profileRepository.findProfile(anonymousId)
            .orElseGet(() -> new RecommendationProfile(anonymousId, Map.of()));
        Map<String, Integer> merged = new LinkedHashMap<>(profile.tagWeights());
        delta.forEach((key, value) -> merged.merge(key, value, Integer::sum));
        profileRepository.updateWeights(anonymousId, merged);
        profileRepository.saveFeedback(
            runId,
            anonymousId,
            request.movieId(),
            request.showtimeId(),
            action,
            delta
        );
        return new FeedbackResponse(true, delta);
    }

    private List<RecommendationItem> itemsFromAi(List<ScoredCandidate> candidates, List<AiPick> picks) {
        Map<Long, ScoredCandidate> byShowtime = candidates.stream()
            .collect(Collectors.toMap(candidate -> candidate.candidate().showtimeId(), Function.identity()));
        List<RecommendationItem> items = new ArrayList<>();
        for (AiPick pick : picks) {
            ScoredCandidate scored = byShowtime.remove(pick.showtimeId());
            if (scored != null) {
                items.add(toItem(scored, pick.reason(), pick.caution(), pick.valuePoint()));
            }
            if (items.size() >= RESULT_LIMIT) {
                return items;
            }
        }
        byShowtime.values().stream()
            .limit(RESULT_LIMIT - items.size())
            .map(this::fallbackItem)
            .forEach(items::add);
        return items;
    }

    private List<RecommendationItem> fallbackItems(List<ScoredCandidate> candidates) {
        return candidates.stream()
            .limit(RESULT_LIMIT)
            .map(this::fallbackItem)
            .toList();
    }

    private RecommendationItem fallbackItem(ScoredCandidate scored) {
        ShowtimeCandidate candidate = scored.candidate();
        String reason = "입력한 취향과 현재 상영 조건에 가장 가깝게 맞는 후보야.";
        if (!scored.matchedTags().isEmpty()) {
            reason = "선택한 분위기와 겹치는 신호가 많아서 우선 추천했어.";
        }
        String caution = scored.penalties().isEmpty()
            ? "큰 주의 요소는 감지되지 않았어."
            : "피하고 싶은 요소와 일부 겹치는 점이 있어.";
        return toItem(scored, reason, caution, valuePoint(candidate));
    }

    private RecommendationItem toItem(ScoredCandidate scored, String reason, String caution, String valuePoint) {
        ShowtimeCandidate candidate = scored.candidate();
        return new RecommendationItem(
            candidate.movieId(),
            candidate.showtimeId(),
            candidate.title(),
            scored.score(),
            blankToDefault(reason, "취향과 상영 조건을 함께 봤을 때 가장 가까운 선택이야."),
            blankToDefault(caution, "큰 주의 요소는 없어."),
            blankToDefault(valuePoint, valuePoint(candidate)),
            candidate.providerCode(),
            candidate.theaterName(),
            candidate.regionName(),
            candidate.screenName(),
            candidate.showDate(),
            candidate.startsAt(),
            candidate.minPriceAmount(),
            candidate.currencyCode(),
            candidate.bookingUrl(),
            candidate.posterUrl()
        );
    }

    private String valuePoint(ShowtimeCandidate candidate) {
        StringBuilder builder = new StringBuilder();
        if (candidate.minPriceAmount() != null) {
            builder.append(String.format("%,d원 기준으로 비교할 수 있어. ", candidate.minPriceAmount()));
        }
        if (candidate.startsAt() != null) {
            builder.append(DateTimeFormatter.ofPattern("HH:mm").format(candidate.startsAt())).append(" 상영이 있어.");
        }
        if (builder.isEmpty()) {
            return "상영관과 예매 정보를 바로 확인할 수 있어.";
        }
        return builder.toString().trim();
    }

    private Map<String, Integer> feedbackDelta(ShowtimeCandidate candidate, FeedbackAction action) {
        int direction = switch (action) {
            case LIKE -> 2;
            case DISLIKE -> -3;
            case BOOKING_VIEW -> 3;
        };
        Map<String, Integer> delta = new LinkedHashMap<>();
        candidate.allTags().forEach(tag -> delta.put(tag, direction));
        return delta;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private long elapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}

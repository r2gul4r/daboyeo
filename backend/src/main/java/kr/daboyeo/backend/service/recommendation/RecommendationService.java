package kr.daboyeo.backend.service.recommendation;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
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
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.SearchFilters;
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
    private static final int RESULT_LIMIT = 3;
    private static final Pattern INTERNAL_TEXT_PATTERN = Pattern.compile(
        "(?i)\\bscore\\s*[:=]?\\s*\\d+\\b|\\bmatchedtags\\b|\\bpenalties\\b|\\b(?:audience|mood|genre|content|pace):[a-z0-9_]+\\b"
    );
    private static final Pattern GENERIC_REASON_PATTERN = Pattern.compile(
        "(선택한\\s*분위기|겹치는\\s*신호|신호가\\s*있|우선\\s*추천|분위기\\s*잘\\s*맞|조건과\\s*가까운\\s*후보)"
    );
    private static final Pattern USER_TAG_PATTERN = Pattern.compile("#[\\p{L}\\p{N}:_\\-]+");

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
        SearchFilters searchFilters = request.searchFilters();
        RecommendationRequest normalizedRequest = new RecommendationRequest(
            resolvedAnonymousId,
            mode.wireValue(),
            request.survey(),
            request.posterChoices(),
            searchFilters
        );
        profileRepository.upsertSurvey(resolvedAnonymousId, normalizedRequest);

        String runId = newRunId();
        int startBufferMinutes = properties.minStartBufferMinutes();
        LocalDateTime minStartsAt = LocalDateTime.now().plusMinutes(startBufferMinutes);
        List<ShowtimeCandidate> candidates = searchFilters != null && searchFilters.active()
            ? showtimeRepository.findUpcomingCandidates(CANDIDATE_LIMIT, minStartsAt, searchFilters)
            : showtimeRepository.findUpcomingCandidates(CANDIDATE_LIMIT, minStartsAt);
        String modelName = properties.modelFor(mode);

        if (candidates.isEmpty()) {
            if (searchFilters != null && searchFilters.active()) {
                return noCandidateResponse(
                    startedAt,
                    runId,
                    resolvedAnonymousId,
                    mode,
                    modelName,
                    normalizedRequest,
                    "no_filtered_candidates",
                    "선택한 지역, 날짜, 시간대, 인원수에 맞는 상영이 없어."
                );
            }
            boolean hasStoredShowtimes = showtimeRepository.countStoredShowtimes() > 0;
            String status = hasStoredShowtimes ? "no_usable_showtimes" : "no_showtime_data";
            String message = hasStoredShowtimes
                ? "수집된 상영 정보는 있지만 지금 예매하기에 충분히 여유 있는 후보가 없어."
                : "수집된 상영 정보가 아직 없어. 수집 스크립트를 먼저 실행해줘.";
            return noCandidateResponse(
                startedAt,
                runId,
                resolvedAnonymousId,
                mode,
                modelName,
                normalizedRequest,
                status,
                message
            );
        }

        List<ScoredCandidate> scored = scorer.score(tagProfile, candidates, searchFilters);
        if (scored.isEmpty()) {
            return noCandidateResponse(
                startedAt,
                runId,
                resolvedAnonymousId,
                mode,
                modelName,
                normalizedRequest,
                "no_matching_candidates",
                "조건에 맞는 상영 정보가 부족해. 피하고 싶은 요소를 조금 줄이면 후보가 늘어날 수 있어."
            );
        }

        List<ScoredCandidate> aiCandidates = selectDistinctMovieItems(scored, properties.aiCandidateLimitFor(mode));
        var aiResult = localModelClient.rankAndExplain(mode, tagProfile, aiCandidates);
        List<RecommendationItem> items = aiResult
            .map(result -> itemsFromAi(scored, result.picks(), tagProfile, mode))
            .filter(list -> !list.isEmpty())
            .orElseGet(() -> fallbackItems(scored, tagProfile, mode));

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

    private RecommendationResponse noCandidateResponse(
        Instant startedAt,
        String runId,
        String anonymousId,
        RecommendationMode mode,
        String modelName,
        RecommendationRequest normalizedRequest,
        String status,
        String message
    ) {
        RecommendationResponse response = new RecommendationResponse(
            runId,
            mode.wireValue(),
            modelName,
            status,
            message,
            List.of()
        );
        profileRepository.saveRun(
            runId,
            anonymousId,
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

    private List<RecommendationItem> itemsFromAi(
        List<ScoredCandidate> rankedCandidates,
        List<AiPick> picks,
        TagProfile profile,
        RecommendationMode mode
    ) {
        Map<Long, ScoredCandidate> byShowtime = rankedCandidates.stream()
            .collect(Collectors.toMap(
                candidate -> candidate.candidate().showtimeId(),
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<Long, AiPick> pickByShowtime = picks.stream()
            .collect(Collectors.toMap(
                AiPick::showtimeId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        List<ScoredCandidate> ordered = new ArrayList<>();
        for (AiPick pick : picks) {
            ScoredCandidate scored = byShowtime.remove(pick.showtimeId());
            if (scored != null) {
                ordered.add(scored);
            }
        }
        ordered.addAll(byShowtime.values());
        return selectDistinctMovieItems(ordered)
            .stream()
            .map(scored -> {
                AiPick pick = pickByShowtime.get(scored.candidate().showtimeId());
                if (pick == null) {
                    return fallbackItem(scored, profile, mode);
                }
                return toItem(
                    scored,
                    qualityReason(pick.reason(), scored),
                    pick.caution(),
                    qualityValuePoint(pick.valuePoint(), scored.candidate()),
                    profile,
                    mode,
                    qualityAnalysisPoint(pick.analysisPoint(), scored, profile, mode)
                );
            })
            .toList();
    }

    private List<RecommendationItem> fallbackItems(List<ScoredCandidate> rankedCandidates, TagProfile profile, RecommendationMode mode) {
        return selectDistinctMovieItems(rankedCandidates)
            .stream()
            .map(scored -> fallbackItem(scored, profile, mode))
            .toList();
    }

    private String qualityReason(String reason, ScoredCandidate scored) {
        String sanitized = sanitizeUserFacingText(reason);
        if (isWeakReason(sanitized, scored.candidate())) {
            return groundedReason(scored);
        }
        String tags = normalizeReasonTags(sanitized);
        return tags.isBlank() ? groundedReason(scored) : tags;
    }

    private boolean isWeakReason(String reason, ShowtimeCandidate candidate) {
        if (reason == null || reason.isBlank()) {
            return true;
        }
        String normalized = reason.trim();
        if (sameText(normalized, candidate.title())) {
            return true;
        }
        return GENERIC_REASON_PATTERN.matcher(normalized).find() || !hasUserFacingTag(normalized);
    }

    private String groundedReason(ScoredCandidate scored) {
        ShowtimeCandidate candidate = scored.candidate();
        List<String> tags = new ArrayList<>();
        scored.matchedTags().stream()
            .filter(this::isReasonSourceTag)
            .map(this::tagPhrase)
            .filter(value -> !value.isBlank())
            .forEach(tags::add);
        candidate.allTags().stream()
            .filter(this::isReasonSourceTag)
            .map(this::tagPhrase)
            .filter(value -> !value.isBlank())
            .forEach(tags::add);
        if (candidate.runtimeMinutes() != null && candidate.runtimeMinutes() <= 125) {
            tags.add("#부담적은러닝타임");
        }
        if (!candidate.ageRating().isBlank()) {
            tags.add(ageTag(candidate.ageRating()));
        }
        if (tags.isEmpty()) {
            tags.add("#조건근접");
        }
        return joinTags(tags, 4);
    }

    private String qualityValuePoint(String valuePoint, ShowtimeCandidate candidate) {
        String sanitized = sanitizeUserFacingText(valuePoint);
        if (isWeakValuePoint(sanitized, candidate)) {
            return groundedValuePoint(candidate);
        }
        String tags = normalizeValueTags(sanitized);
        return tags.isBlank() ? groundedValuePoint(candidate) : tags;
    }

    private boolean isWeakValuePoint(String valuePoint, ShowtimeCandidate candidate) {
        if (valuePoint == null || valuePoint.isBlank()) {
            return true;
        }
        String normalized = valuePoint.trim();
        if (sameText(normalized, candidate.theaterName())
            || sameText(normalized, candidate.screenName())
            || sameText(normalized, candidate.regionName())) {
            return true;
        }
        return !hasUserFacingTag(normalized);
    }

    private String groundedValuePoint(ShowtimeCandidate candidate) {
        List<String> tags = new ArrayList<>();
        if (candidate.startsAt() != null) {
            tags.add("#" + DateTimeFormatter.ofPattern("HH:mm").format(candidate.startsAt()) + "상영");
        }
        if (candidate.minPriceAmount() != null) {
            tags.add("#" + candidate.minPriceAmount() + "원");
        }
        String seat = seatHint(candidate.remainingSeatCount(), candidate.totalSeatCount());
        if ("enough".equals(seat)) {
            tags.add("#좌석여유");
        } else if ("limited".equals(seat)) {
            tags.add("#좌석주의");
        }
        if (!candidate.theaterName().isBlank()) {
            tags.add("#예매가능");
        }
        if (tags.isEmpty()) {
            tags.add("#예매정보");
        }
        return joinTags(tags, 4);
    }

    private String ageTag(String ageRating) {
        String age = ageRating == null ? "" : ageRating.trim();
        if (age.isBlank()) {
            return "";
        }
        String compact = age
            .replace("이상", "")
            .replace("관람가", "")
            .replace("관람", "")
            .replaceAll("\\s+", "");
        if (compact.matches("\\d+")) {
            compact = compact + "세";
        }
        return "#" + compact;
    }

    private String tagPhrase(String tag) {
        if (tag == null || tag.isBlank()) {
            return "";
        }
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "audience:alone" -> "#혼영";
            case "audience:friends" -> "#친구랑";
            case "audience:date" -> "#데이트";
            case "audience:family" -> "#가족";
            case "audience:child" -> "#아이와함께";
            case "mood:light" -> "#가볍게";
            case "mood:immersive" -> "#몰입";
            case "mood:exciting" -> "#신나는";
            case "mood:calm" -> "#잔잔한";
            case "mood:tense" -> "#긴장감";
            case "mood:warm" -> "#따뜻한";
            case "mood:visual" -> "#시각적재미";
            case "pace:easy" -> "#이해쉬움";
            case "pace:fast" -> "#빠른전개";
            case "pace:slow" -> "#천천히몰입";
            case "content:too_long" -> "#긴영화주의";
            case "content:complex" -> "#난도주의";
            case "content:violence" -> "#잔인함주의";
            case "content:sad_ending" -> "#슬픈결말주의";
            case "content:loud" -> "#큰소리주의";
            default -> {
                if (normalized.startsWith("genre:")) {
                    yield "#" + normalized.substring("genre:".length()).replace('_', '-');
                }
                yield "";
            }
        };
    }

    private boolean isReasonSourceTag(String tag) {
        return tag != null && !tag.trim().toLowerCase(Locale.ROOT).startsWith("content:");
    }

    private String genreLabel(String genre) {
        String normalized = genre == null ? "" : genre.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "action" -> "액션";
            case "adventure" -> "어드벤처";
            case "animation" -> "애니메이션";
            case "comedy" -> "코미디";
            case "crime" -> "범죄";
            case "drama" -> "드라마";
            case "family" -> "가족";
            case "fantasy" -> "판타지";
            case "history" -> "역사";
            case "horror" -> "공포";
            case "music" -> "음악";
            case "musical" -> "뮤지컬";
            case "mystery" -> "미스터리";
            case "romance" -> "로맨스";
            case "sf", "sci-fi", "science-fiction" -> "SF";
            case "thriller" -> "스릴러";
            default -> normalized.isBlank() ? "장르" : normalized;
        };
    }

    private String seatHint(Integer remainingSeatCount, Integer totalSeatCount) {
        if (remainingSeatCount == null) {
            return "";
        }
        if (remainingSeatCount <= 0) {
            return "none";
        }
        if (totalSeatCount == null || totalSeatCount <= 0) {
            return remainingSeatCount >= 20 ? "enough" : "limited";
        }
        double ratio = remainingSeatCount / (double) totalSeatCount;
        if (remainingSeatCount >= 30 || ratio >= 0.3) {
            return "enough";
        }
        if (remainingSeatCount <= 10 || ratio <= 0.1) {
            return "limited";
        }
        return "normal";
    }

    private String joinTags(List<String> tags, int limit) {
        return tags.stream()
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .limit(limit)
            .collect(Collectors.joining(" "));
    }

    private boolean hasUserFacingTag(String value) {
        return value != null && USER_TAG_PATTERN.matcher(value).find();
    }

    private String normalizeReasonTags(String value) {
        return joinTags(extractTags(value).stream()
            .filter(tag -> !isValueTag(tag))
            .toList(), 4);
    }

    private String normalizeValueTags(String value) {
        return joinTags(extractTags(value).stream()
            .filter(this::isValueTag)
            .toList(), 4);
    }

    private List<String> extractTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        var matcher = USER_TAG_PATTERN.matcher(value);
        while (matcher.find()) {
            tags.add(matcher.group());
        }
        return tags;
    }

    private boolean isValueTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        return tag.matches("#\\d{1,2}:\\d{2}(상영)?")
            || tag.matches("#\\d+원")
            || tag.equals("#좌석여유")
            || tag.equals("#좌석주의")
            || tag.equals("#예매가능")
            || tag.equals("#예매정보");
    }

    private boolean sameText(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private List<ScoredCandidate> selectDistinctMovieItems(List<ScoredCandidate> rankedCandidates) {
        return selectDistinctMovieItems(rankedCandidates, RESULT_LIMIT);
    }

    private List<ScoredCandidate> selectDistinctMovieItems(List<ScoredCandidate> rankedCandidates, int limit) {
        List<ScoredCandidate> selected = new ArrayList<>();
        List<ScoredCandidate> deferred = new ArrayList<>();
        Set<String> seenMovies = new LinkedHashSet<>();
        for (ScoredCandidate scored : rankedCandidates) {
            if (selected.size() >= limit) {
                break;
            }
            String movieKey = movieKey(scored);
            if (seenMovies.add(movieKey)) {
                selected.add(scored);
            } else {
                deferred.add(scored);
            }
        }
        if (selected.size() < limit) {
            for (ScoredCandidate scored : deferred) {
                if (selected.size() >= limit) {
                    break;
                }
                selected.add(scored);
            }
        }
        return selected;
    }

    private RecommendationItem fallbackItem(ScoredCandidate scored, TagProfile profile, RecommendationMode mode) {
        ShowtimeCandidate candidate = scored.candidate();
        return toItem(scored, groundedReason(scored), "", groundedValuePoint(candidate), profile, mode, qualityAnalysisPoint("", scored, profile, mode));
    }

    private RecommendationItem toItem(
        ScoredCandidate scored,
        String reason,
        String caution,
        String valuePoint,
        TagProfile profile,
        RecommendationMode mode,
        String analysisPoint
    ) {
        ShowtimeCandidate candidate = scored.candidate();
        return new RecommendationItem(
            candidate.movieId(),
            candidate.showtimeId(),
            candidate.title(),
            scored.score(),
            blankToDefault(sanitizeUserFacingText(reason), "#조건근접"),
            mode == RecommendationMode.PRECISE ? blankToDefault(sanitizeUserFacingText(analysisPoint), analysisPoint(scored, profile)) : "",
            blankToDefault(sanitizeUserFacingText(caution), ""),
            blankToDefault(sanitizeUserFacingText(valuePoint), valuePoint(candidate)),
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
        return groundedValuePoint(candidate);
    }

    private String qualityAnalysisPoint(
        String analysisPoint,
        ScoredCandidate scored,
        TagProfile profile,
        RecommendationMode mode
    ) {
        if (mode != RecommendationMode.PRECISE) {
            return "";
        }
        String sanitized = sanitizeUserFacingText(analysisPoint);
        String tag = extractTags(sanitized).stream()
            .filter(this::isAnalysisTag)
            .findFirst()
            .orElse("");
        return tag.isBlank() ? analysisPoint(scored, profile) : tag;
    }

    private boolean isAnalysisTag(String tag) {
        if (tag == null || tag.isBlank() || isValueTag(tag)) {
            return false;
        }
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        int length = tag.codePointCount(0, tag.length());
        return length <= 18
            && !normalized.contains("_")
            && !normalized.contains("주의")
            && !normalized.contains("잔인")
            && !normalized.contains("폭력")
            && !normalized.contains("시끄")
            && !normalized.contains("슬픔")
            && !normalized.contains("긴영화");
    }

    private String analysisPoint(ScoredCandidate scored, TagProfile profile) {
        ShowtimeCandidate candidate = scored.candidate();
        Set<String> likedGenres = profile == null ? Set.of() : profile.likedGenres();
        for (String tag : candidate.allTags()) {
            String normalized = tag == null ? "" : tag.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("genre:") && likedGenres.contains(normalized)) {
                return "#" + genreLabel(normalized.substring("genre:".length())) + "취향";
            }
        }
        if (!likedGenres.isEmpty()) {
            String preferredGenre = likedGenres.iterator().next();
            return "#" + genreLabel(preferredGenre.substring("genre:".length())) + "취향";
        }
        return candidate.allTags().stream()
            .filter(tag -> tag != null && tag.toLowerCase(Locale.ROOT).startsWith("genre:"))
            .map(tag -> "#" + genreLabel(tag.substring("genre:".length())) + "계열")
            .findFirst()
            .orElse("#장르근접");
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

    private String sanitizeUserFacingText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = INTERNAL_TEXT_PATTERN.matcher(value).replaceAll(" ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        return sanitized.replaceAll("[\\p{Punct}\\s]+", "").isBlank() ? "" : sanitized;
    }

    private String movieKey(ScoredCandidate scored) {
        ShowtimeCandidate candidate = scored.candidate();
        String title = candidate.title();
        if (title != null && !title.isBlank()) {
            return "title:" + title.trim().toLowerCase(Locale.ROOT);
        }
        String externalMovieId = candidate.externalMovieId();
        if (externalMovieId != null && !externalMovieId.isBlank()) {
            return candidate.providerCode() + ":" + externalMovieId.trim();
        }
        Long movieId = candidate.movieId();
        return movieId == null ? "showtime:" + candidate.showtimeId() : "movie:" + movieId;
    }

    private long elapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}

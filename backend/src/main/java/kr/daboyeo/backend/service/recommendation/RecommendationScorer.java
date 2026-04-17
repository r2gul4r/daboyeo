package kr.daboyeo.backend.service.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ScoredCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import org.springframework.stereotype.Component;

@Component
public class RecommendationScorer {

    private static final int BASE_SCORE = 50;

    public List<ScoredCandidate> score(TagProfile profile, List<ShowtimeCandidate> candidates) {
        return candidates.stream()
            .map(candidate -> scoreOne(profile, candidate))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
            .toList();
    }

    public Optional<ScoredCandidate> scoreOne(TagProfile profile, ShowtimeCandidate candidate) {
        if (isBlockedForChild(profile, candidate)) {
            return Optional.empty();
        }

        int score = BASE_SCORE;
        List<String> matchedTags = new ArrayList<>();
        List<String> penalties = new ArrayList<>();

        for (String tag : candidate.allTags()) {
            int weight = profile.weight(tag);
            if (weight != 0) {
                score += weight * 4;
                matchedTags.add(tag);
            }
        }

        score += priceBonus(candidate);
        score += seatBonus(candidate);
        score += timeBonus(profile, candidate);
        score -= avoidPenalty(profile, candidate, penalties);

        if (candidate.runtimeMinutes() != null && candidate.runtimeMinutes() <= 110) {
            score += 3;
        }

        int bounded = Math.max(0, Math.min(100, score));
        return Optional.of(new ScoredCandidate(candidate, bounded, matchedTags, penalties));
    }

    private boolean isBlockedForChild(TagProfile profile, ShowtimeCandidate candidate) {
        if (!"child".equals(profile.audience())) {
            return false;
        }
        String age = normalize(candidate.ageRating());
        if (isUnsafeForChild(age)) {
            return true;
        }
        return candidate.allTags().stream().anyMatch(tag ->
            tag.equals("genre:horror")
                || tag.equals("content:violence")
                || tag.equals("content:adult")
                || tag.equals("content:dark")
        );
    }

    private boolean isUnsafeForChild(String age) {
        if (age.isBlank()) {
            return false;
        }
        if (age.contains("전체") || age.contains("all") || age.contains("12")) {
            return false;
        }
        return age.contains("청불")
            || age.contains("청소년")
            || age.contains("불가")
            || age.contains("adult")
            || age.contains("restricted")
            || age.contains("19")
            || age.contains("18")
            || age.contains("15");
    }

    private int priceBonus(ShowtimeCandidate candidate) {
        Integer price = candidate.minPriceAmount();
        if (price == null || price <= 0) {
            return 0;
        }
        if (price <= 10_000) {
            return 8;
        }
        if (price <= 13_000) {
            return 5;
        }
        if (price <= 16_000) {
            return 2;
        }
        return 0;
    }

    private int seatBonus(ShowtimeCandidate candidate) {
        Integer remaining = candidate.remainingSeatCount();
        Integer total = candidate.totalSeatCount();
        if (remaining == null || total == null || total <= 0) {
            return 0;
        }
        double ratio = remaining / (double) total;
        if (ratio >= 0.4) {
            return 5;
        }
        if (ratio >= 0.2) {
            return 2;
        }
        return -3;
    }

    private int timeBonus(TagProfile profile, ShowtimeCandidate candidate) {
        if (candidate.startsAt() == null) {
            return 0;
        }
        int hour = candidate.startsAt().getHour();
        int value = 0;
        if (hour >= 1 && hour < 6) {
            value -= 6;
        }
        if ("child".equals(profile.audience()) && hour >= 20) {
            value -= 10;
        } else if ("family".equals(profile.audience()) && hour >= 21) {
            value -= 6;
        }
        if ("light".equals(profile.mood()) && hour >= 10 && hour <= 22) {
            value += 2;
        }
        if ("exciting".equals(profile.mood()) && hour >= 17 && hour <= 23) {
            value += 2;
        }
        if ("calm".equals(profile.mood()) && hour >= 9 && hour <= 18) {
            value += 2;
        }
        return value;
    }

    private int avoidPenalty(TagProfile profile, ShowtimeCandidate candidate, List<String> penalties) {
        int penalty = 0;
        if (profile.avoids("too_long") && candidate.runtimeMinutes() != null && candidate.runtimeMinutes() > 130) {
            int value = Math.min(25, 10 + ((candidate.runtimeMinutes() - 130) / 5));
            penalty += value;
            penalties.add("too_long");
        }
        if (profile.avoids("violence") && hasAny(candidate, "content:violence", "genre:horror", "genre:thriller")) {
            penalty += 22;
            penalties.add("violence");
        }
        if (profile.avoids("complex") && hasAny(candidate, "content:complex", "mood:serious", "mood:immersive")) {
            penalty += 12;
            penalties.add("complex");
        }
        if (profile.avoids("sad_ending") && hasAny(candidate, "content:sad_ending", "mood:sad")) {
            penalty += 14;
            penalties.add("sad_ending");
        }
        if (profile.avoids("loud") && hasAny(candidate, "content:loud", "mood:exciting")) {
            penalty += 9;
            penalties.add("loud");
        }
        return penalty;
    }

    private boolean hasAny(ShowtimeCandidate candidate, String... tags) {
        var all = candidate.allTags();
        for (String tag : tags) {
            if (all.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

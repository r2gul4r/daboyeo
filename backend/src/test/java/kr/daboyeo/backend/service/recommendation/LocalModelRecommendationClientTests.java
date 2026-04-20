package kr.daboyeo.backend.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.daboyeo.backend.config.RecommendationProperties;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiPick;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.AiResult;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.RecommendationMode;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ScoredCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.ShowtimeCandidate;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.TagProfile;
import org.junit.jupiter.api.Test;

class LocalModelRecommendationClientTests {

    private final LocalModelRecommendationClient client = new LocalModelRecommendationClient(properties(), new ObjectMapper());

    @Test
    void buildPromptDoesNotExposeInternalTokens() {
        TagProfile profile = new TagProfile();
        profile.setAudience("friends");
        profile.setMood("exciting");
        profile.addAvoid(List.of("too_long"));
        String prompt = client.buildPrompt(RecommendationMode.FAST, profile, List.of(scoredCandidate()));

        assertThat(prompt).contains("Test Movie", "\"id\":1", "\"b\"", "\"vp\"");
        assertThat(prompt).contains("#신나는", "#좌석여유", "#12세");
        assertThat(prompt).doesNotContain("#큰소리주의");
        assertThat(prompt).contains("Return only", "why=b tags only", "v=vp tags only");
        assertThat(prompt).doesNotContain("\"showtimeId\"", "\"recommendations\"", "\"reason\"", "\"valuePoint\"");
        assertThat(prompt).doesNotContain("\"score\"", "\"matchedTags\"", "\"penalties\"", "\"tg\"", "\"rt\"", "\"age\"", "\"th\"", "\"time\"", "\"price\"", "\"seat\"");
        assertThat(prompt).doesNotContain("caution");
        assertThat(prompt.length()).isLessThan(900);
    }

    @Test
    void precisePromptRequestsCompactAiAnalysisResponse() {
        TagProfile profile = new TagProfile();
        profile.setAudience("friends");
        profile.setMood("exciting");
        profile.addAvoid(List.of("too_long"));
        profile.addLikedGenre("sf");

        String prompt = client.buildPrompt(RecommendationMode.PRECISE, profile, List.of(scoredCandidate()));

        assertThat(prompt).contains("Test Movie", "\"id\":1", "\"b\"", "\"vp\"");
        assertThat(prompt).contains(
            "Return only {\"r\":[{\"id\":1,\"a\":\"#SF취향\"}]}",
            "Use b first",
            "liked=[#SF취향]",
            "copy one hashtag from liked"
        );
        assertThat(prompt).doesNotContain("\"why\"", "\"v\"", "\"score\"", "\"matchedTags\"", "\"penalties\"", "caution");
        assertThat(prompt.length()).isLessThan(850);
    }

    @Test
    void compactAiResponseIsMappedToExistingAiPickShape() throws Exception {
        String json = "{\"r\":[{\"id\":1,\"why\":\"#가볍게 #친구랑\",\"v\":\"#17:00상영 #좌석여유\"}]}";

        AiResult result = client.parseResult(json, "gemma-test").orElseThrow();

        AiPick pick = result.picks().get(0);
        assertThat(pick.showtimeId()).isEqualTo(1L);
        assertThat(pick.reason()).isEqualTo("#가볍게 #친구랑");
        assertThat(pick.caution()).isBlank();
        assertThat(pick.valuePoint()).isEqualTo("#17:00상영 #좌석여유");
        assertThat(pick.analysisPoint()).isBlank();
        assertThat(result.rawJson()).isEqualTo(json);
    }

    @Test
    void preciseAnalysisResponseIsMappedToAiPickAnalysisPoint() throws Exception {
        String json = "{\"r\":[{\"id\":1,\"a\":\"#SF취향\"},{\"id\":2,\"a\":\"#몰입취향\"}]}";

        AiResult result = client.parseResult(json, "gemma-test").orElseThrow();

        assertThat(result.picks()).extracting(AiPick::showtimeId).containsExactly(1L, 2L);
        assertThat(result.picks()).extracting(AiPick::analysisPoint).containsExactly("#SF취향", "#몰입취향");
        assertThat(result.picks()).allSatisfy(pick -> {
            assertThat(pick.reason()).isBlank();
            assertThat(pick.valuePoint()).isBlank();
            assertThat(pick.caution()).isBlank();
        });
        assertThat(result.rawJson()).isEqualTo(json);
    }

    private RecommendationProperties properties() {
        return new RecommendationProperties(
            null,
            null,
            null,
            20,
            5,
            5,
            280,
            96,
            56,
            List.of("http://localhost:5173")
        );
    }

    private ScoredCandidate scoredCandidate() {
        ShowtimeCandidate candidate = new ShowtimeCandidate(
            10L,
            1L,
            "Test Movie",
            "MEGABOX",
            "movie-1",
            "Test Theater",
            "Seoul",
            "1관",
            "IMAX",
            "IMAX",
            LocalDate.now(),
            LocalDateTime.now().plusHours(2),
            null,
            50,
            100,
            12_000,
            "KRW",
            "https://example.test/booking",
            "https://example.test/poster.jpg",
            "12",
            120,
            Set.of("mood:exciting", "content:loud")
        );
        return new ScoredCandidate(candidate, 96, List.of("mood:exciting"), List.of("content:loud"));
    }
}

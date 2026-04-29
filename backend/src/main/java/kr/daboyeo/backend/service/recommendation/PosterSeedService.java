package kr.daboyeo.backend.service.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.PosterSeedMovie;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PosterSeedService {

    private static final int MAX_LIMIT = 40;
    private static final String LOCAL_POSTER_MANIFEST = "recommendation/korea-boxoffice-top50-posters.json";
    private static final List<String> DEFAULT_GENRES = List.of("popular");
    private static final List<String> DEFAULT_MOODS = List.of("visual", "immersive");
    private static final List<String> DEFAULT_AUDIENCES = List.of("alone", "friends", "family");

    private final List<PosterSeedMovie> seedMovies;
    private final Map<String, PosterSeedMovie> seedById;
    private final SecureRandom random = new SecureRandom();

    public PosterSeedService(ObjectMapper objectMapper) {
        this.seedMovies = loadSeed(objectMapper);
        this.seedById = seedMovies.stream()
            .collect(Collectors.toUnmodifiableMap(PosterSeedMovie::id, Function.identity(), (left, right) -> left));
    }

    public List<PosterSeedMovie> randomSeed(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, Math.min(MAX_LIMIT, seedMovies.size())));
        List<PosterSeedMovie> copy = new ArrayList<>(seedMovies);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            PosterSeedMovie current = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, current);
        }
        return copy.stream().limit(boundedLimit).toList();
    }

    public Optional<PosterSeedMovie> findById(String id) {
        return Optional.ofNullable(seedById.get(id));
    }

    private List<PosterSeedMovie> loadSeed(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(LOCAL_POSTER_MANIFEST);
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode moviesNode = objectMapper.readTree(inputStream).path("movies");
            if (!moviesNode.isArray()) {
                throw new IllegalStateException("R2 poster manifest has no movies array.");
            }
            List<PosterSeedMovie> movies = StreamSupport.stream(moviesNode.spliterator(), false)
                .map(this::toPosterSeedMovie)
                .filter(movie -> !movie.id().isBlank() && !movie.posterUrl().isBlank())
                .toList();
            return List.copyOf(movies);
        } catch (IOException e) {
            throw new IllegalStateException("R2 poster seed data could not be loaded.", e);
        }
    }

    private PosterSeedMovie toPosterSeedMovie(JsonNode node) {
        String movieCode = text(node, "movieCd");
        String rank = text(node, "rank");
        String id = movieCode.isBlank() ? "kobis-rank-" + rank : movieCode;
        return new PosterSeedMovie(
            id,
            text(node, "titleKo"),
            text(node, "posterPath"),
            DEFAULT_GENRES,
            DEFAULT_MOODS,
            paceForRank(node.path("rank").asInt(0)),
            DEFAULT_AUDIENCES,
            List.of(),
            ""
        );
    }

    private String paceForRank(int rank) {
        if (rank > 0 && rank % 3 == 0) {
            return "fast";
        }
        if (rank > 0 && rank % 5 == 0) {
            return "slow";
        }
        return "medium";
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }
}

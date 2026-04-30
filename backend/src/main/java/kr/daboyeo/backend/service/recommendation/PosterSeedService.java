package kr.daboyeo.backend.service.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final String LOCAL_POSTER_TAGS = "recommendation/korea-boxoffice-top50-poster-tags.json";
    private static final List<String> DEFAULT_GENRES = List.of("popular");
    private static final List<String> DEFAULT_MOODS = List.of();
    private static final List<String> DEFAULT_AUDIENCES = List.of();
    private static final PosterSeedTags DEFAULT_TAGS = new PosterSeedTags(
        DEFAULT_GENRES,
        DEFAULT_MOODS,
        "",
        DEFAULT_AUDIENCES,
        List.of(),
        ""
    );

    private final List<PosterSeedMovie> seedMovies;
    private final Map<String, PosterSeedMovie> seedById;
    private final SecureRandom random = new SecureRandom();

    public PosterSeedService(ObjectMapper objectMapper) {
        this.seedMovies = loadSeed(objectMapper, loadTags(objectMapper));
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

    private List<PosterSeedMovie> loadSeed(ObjectMapper objectMapper, Map<String, PosterSeedTags> seedTags) {
        ClassPathResource resource = new ClassPathResource(LOCAL_POSTER_MANIFEST);
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode moviesNode = objectMapper.readTree(inputStream).path("movies");
            if (!moviesNode.isArray()) {
                throw new IllegalStateException("R2 poster manifest has no movies array.");
            }
            List<PosterSeedMovie> movies = StreamSupport.stream(moviesNode.spliterator(), false)
                .map(node -> toPosterSeedMovie(node, seedTags))
                .filter(movie -> !movie.id().isBlank() && !movie.posterUrl().isBlank())
                .toList();
            return List.copyOf(movies);
        } catch (IOException e) {
            throw new IllegalStateException("R2 poster seed data could not be loaded.", e);
        }
    }

    private Map<String, PosterSeedTags> loadTags(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(LOCAL_POSTER_TAGS);
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode moviesNode = objectMapper.readTree(inputStream).path("movies");
            if (!moviesNode.isObject()) {
                throw new IllegalStateException("R2 poster tag data has no movies object.");
            }
            Map<String, PosterSeedTags> tagsByMovieCode = new LinkedHashMap<>();
            moviesNode.fields().forEachRemaining(entry ->
                tagsByMovieCode.put(entry.getKey(), toPosterSeedTags(entry.getValue()))
            );
            return Map.copyOf(tagsByMovieCode);
        } catch (IOException e) {
            throw new IllegalStateException("R2 poster tag data could not be loaded.", e);
        }
    }

    private PosterSeedMovie toPosterSeedMovie(JsonNode node, Map<String, PosterSeedTags> seedTags) {
        String movieCode = text(node, "movieCd");
        String rank = text(node, "rank");
        String id = movieCode.isBlank() ? "kobis-rank-" + rank : movieCode;
        PosterSeedTags tags = seedTags.getOrDefault(movieCode, DEFAULT_TAGS);
        return new PosterSeedMovie(
            id,
            text(node, "titleKo"),
            text(node, "posterPath"),
            tags.genres(),
            tags.moods(),
            tags.pace(),
            tags.audiences(),
            tags.avoid(),
            tags.ageRating()
        );
    }

    private PosterSeedTags toPosterSeedTags(JsonNode node) {
        return new PosterSeedTags(
            stringList(node.path("genres")),
            stringList(node.path("moods")),
            text(node, "pace"),
            stringList(node.path("audiences")),
            stringList(node.path("avoid")),
            text(node, "ageRating")
        );
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(node.spliterator(), false)
            .map(value -> value.asText(""))
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private record PosterSeedTags(
        List<String> genres,
        List<String> moods,
        String pace,
        List<String> audiences,
        List<String> avoid,
        String ageRating
    ) {
        private PosterSeedTags {
            genres = genres == null ? List.of() : List.copyOf(genres);
            moods = moods == null ? List.of() : List.copyOf(moods);
            pace = pace == null ? "" : pace.trim();
            audiences = audiences == null ? List.of() : List.copyOf(audiences);
            avoid = avoid == null ? List.of() : List.copyOf(avoid);
            ageRating = ageRating == null ? "" : ageRating.trim();
        }
    }
}

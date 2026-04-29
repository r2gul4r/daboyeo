package kr.daboyeo.backend.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
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
import kr.daboyeo.backend.domain.recommendation.RecommendationModels.PosterSeedMovie;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PosterSeedService {

    private static final int MAX_LIMIT = 40;

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
        ClassPathResource resource = new ClassPathResource("recommendation/poster-seed.json");
        try (InputStream inputStream = resource.getInputStream()) {
            List<PosterSeedMovie> movies = objectMapper.readValue(
                inputStream,
                new TypeReference<List<PosterSeedMovie>>() {
                }
            );
            return List.copyOf(movies);
        } catch (IOException e) {
            throw new IllegalStateException("포스터 seed 데이터를 읽지 못했어.", e);
        }
    }
}

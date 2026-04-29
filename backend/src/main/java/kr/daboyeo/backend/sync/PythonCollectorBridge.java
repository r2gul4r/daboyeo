package kr.daboyeo.backend.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.config.RootDotenvLoader;
import org.springframework.stereotype.Component;

@Component
public class PythonCollectorBridge {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CollectorSyncProperties properties;
    private final ObjectMapper objectMapper;
    private final Path workspaceRoot;
    private final Map<String, Object> dotenvValues;

    public PythonCollectorBridge(CollectorSyncProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.workspaceRoot = resolveWorkspaceRoot();
        this.dotenvValues = RootDotenvLoader.load(workspaceRoot);
    }

    public Map<String, Object> collectShowtimeBundle(ShowtimeCollectionRequest request) {
        String script = buildShowtimeScript(request.provider());
        Map<String, String> env = switch (request.provider()) {
            case CGV -> Map.of(
                "SITE_NO", request.siteNo(),
                "MOVIE_NO", request.movieNo(),
                "PLAY_DATE", request.playDate().toString().replace("-", "")
            );
            case LOTTE_CINEMA -> Map.of(
                "PLAY_DATE", request.playDate().toString(),
                "CINEMA_SELECTOR", request.cinemaSelector(),
                "REPRESENTATION_MOVIE_CODE", request.representationMovieCode()
            );
            case MEGABOX -> Map.of(
                "PLAY_DATE", request.playDate().toString().replace("-", ""),
                "MOVIE_NO", request.movieNo(),
                "AREA_CODE", request.areaCode()
            );
        };
        return executeJsonScript(script, env);
    }

    public ProviderDiscoveryPayload collectShowtimeDiscovery(CollectorProvider provider, java.time.LocalDate playDate) {
        String script = buildDiscoveryScript(provider);
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PLAY_DATE", provider == CollectorProvider.LOTTE_CINEMA ? playDate.toString() : playDate.toString().replace("-", ""));
        env.put("DISCOVERY_MOVIE_LIMIT", String.valueOf(properties.getShowtimes().getDiscoveryMovieLimit()));
        env.put("DISCOVERY_CINEMA_LIMIT", String.valueOf(properties.getShowtimes().getDiscoveryLotteCinemaLimit()));
        env.put("DISCOVERY_BUNDLE_LIMIT", String.valueOf(properties.getShowtimes().getDiscoveryMegaboxBundleLimit()));
        env.put("PREFERRED_CINEMA_IDS", String.join("||", properties.getShowtimes().getLottePreferredCinemaIds()));
        env.put("PREFERRED_CINEMA_NAMES", String.join("||", properties.getShowtimes().getLottePreferredCinemaNames()));
        env.put("MEGABOX_AREA_CODES", String.join("||", properties.getShowtimes().getMegaboxAreaCodes()));
        Map<String, Object> payload = executeJsonScript(script, env);
        return new ProviderDiscoveryPayload(
            listOfMaps(payload.get("targets"))
        );
    }

    public SeatCollectionResult collectSeatSnapshot(SeatCollectionRequest request) {
        String script = buildSeatScript(request.provider());
        Map<String, String> env = new LinkedHashMap<>();
        request.bookingKey().forEach((key, value) -> {
            if (value != null) {
                env.put(key.toUpperCase(), String.valueOf(value));
            }
        });
        Map<String, Object> payload = executeJsonScript(script, env);
        return new SeatCollectionResult(
            castMap(payload.get("summary")),
            listOfMaps(payload.get("seats"))
        );
    }

    private Map<String, Object> executeJsonScript(String script, Map<String, String> env) {
        ProcessBuilder processBuilder = new ProcessBuilder(properties.getPythonExecutable(), "-c", script);
        processBuilder.directory(workspaceRoot.toFile());
        processBuilder.redirectErrorStream(false);
        Map<String, String> processEnv = processBuilder.environment();
        processEnv.put("PYTHONIOENCODING", "utf-8");
        dotenvValues.forEach((key, value) -> processEnv.putIfAbsent(key, String.valueOf(value)));
        env.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                processEnv.put(key, value);
            }
        });

        Path payloadPath = null;
        try {
            payloadPath = Files.createTempFile("daboyeo-collector-", ".json");
            processEnv.put("OUTPUT_JSON_PATH", payloadPath.toString());
            Process process = processBuilder.start();
            boolean completed = process.waitFor(properties.getProcessTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("Python collector timed out after " + properties.getProcessTimeoutSeconds() + " seconds.");
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Python collector failed: " + stderr);
            }
            String payload = payloadPath != null && Files.exists(payloadPath)
                ? Files.readString(payloadPath, StandardCharsets.UTF_8).trim()
                : stdout;
            if (payload.isBlank()) {
                throw new IllegalStateException("Python collector returned no JSON payload.");
            }
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start python collector.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python collector wait was interrupted.", exception);
        } finally {
            if (payloadPath != null) {
                try {
                    Files.deleteIfExists(payloadPath);
                } catch (IOException ignored) {
                    // Ignore temp file cleanup failures.
                }
            }
        }
    }

    private static Path resolveWorkspaceRoot() {
        Path start = Path.of(System.getProperty("user.dir", "."));
        Path dotenv = RootDotenvLoader.findDotenvPath(start);
        if (dotenv != null && dotenv.getParent() != null) {
            return dotenv.getParent();
        }
        return start.toAbsolutePath().getParent() == null ? start.toAbsolutePath() : start.toAbsolutePath().getParent();
    }

    private static String buildShowtimeScript(CollectorProvider provider) {
        return switch (provider) {
            case CGV -> """
                import json, os
                from pathlib import Path
                from collectors.cgv.collector import CgvCollector
                bundle = CgvCollector().collect_bundle(
                    site_no=os.environ['SITE_NO'],
                    mov_no=os.environ['MOVIE_NO'],
                    scn_ymd=os.environ.get('PLAY_DATE') or None,
                    scns_no=None,
                    scn_sseq=None,
                )
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(json.dumps(bundle, ensure_ascii=False), encoding='utf-8')
                """;
            case LOTTE_CINEMA -> """
                import json, os
                from pathlib import Path
                from collectors.lotte.collector import LotteCinemaCollector
                bundle = LotteCinemaCollector().collect_bundle(
                    play_date=os.environ['PLAY_DATE'],
                    cinema_selector=os.environ['CINEMA_SELECTOR'],
                    representation_movie_code=os.environ['REPRESENTATION_MOVIE_CODE'],
                )
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(json.dumps(bundle, ensure_ascii=False), encoding='utf-8')
                """;
            case MEGABOX -> """
                import json, os
                from pathlib import Path
                from collectors.megabox.collector import MegaboxCollector
                bundle = MegaboxCollector().collect_bundle(
                    play_de=os.environ['PLAY_DATE'],
                    movie_no=os.environ['MOVIE_NO'],
                    area_cd=os.environ['AREA_CODE'],
                )
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(json.dumps(bundle, ensure_ascii=False), encoding='utf-8')
                """;
        };
    }

    private static String buildDiscoveryScript(CollectorProvider provider) {
        return switch (provider) {
            case LOTTE_CINEMA -> """
                import json, os
                from pathlib import Path
                from collectors.lotte.collector import LotteCinemaCollector
                collector = LotteCinemaCollector()
                play_date = os.environ['PLAY_DATE']
                movie_limit = int(os.environ.get('DISCOVERY_MOVIE_LIMIT', '20'))
                cinema_limit = int(os.environ.get('DISCOVERY_CINEMA_LIMIT', '2'))
                preferred_ids = {item.strip() for item in os.environ.get('PREFERRED_CINEMA_IDS', '').split('||') if item.strip()}
                preferred = [item.strip() for item in os.environ.get('PREFERRED_CINEMA_NAMES', '').split('||') if item.strip()]
                movies = [row for row in collector.build_movie_records() if row.get('movie_no')][:movie_limit]
                cinemas = [row for row in collector.build_cinema_records() if row.get('cinema_id')]
                if preferred_ids:
                    cinemas = [row for row in cinemas if str(row.get('cinema_id')) in preferred_ids]
                elif preferred:
                    cinemas = [row for row in cinemas if any(name in str(row.get('cinema_name', '')) for name in preferred)]
                targets = []
                for cinema in cinemas[:cinema_limit]:
                    selector = collector.build_cinema_selector(cinema.get('raw') or {})
                    if not selector:
                        continue
                    for movie in movies:
                        movie_no = str(movie.get('movie_no') or '').strip()
                        if not movie_no:
                            continue
                        schedules = collector.build_schedule_records(
                            play_date=play_date,
                            cinema_selector=selector,
                            representation_movie_code=movie_no,
                        )
                        if schedules:
                            targets.append({
                                'cinema_selector': selector,
                                'representation_movie_code': movie_no,
                                'cinema_name': cinema.get('cinema_name'),
                                'schedule_count': len(schedules),
                            })
                            break
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                    json.dumps({'targets': targets}, ensure_ascii=False),
                    encoding='utf-8'
                )
                """;
            case MEGABOX -> """
                import json, os
                from pathlib import Path
                from collectors.megabox.collector import MegaboxCollector
                collector = MegaboxCollector()
                play_de = os.environ['PLAY_DATE']
                movie_limit = int(os.environ.get('DISCOVERY_MOVIE_LIMIT', '20'))
                bundle_limit = int(os.environ.get('DISCOVERY_BUNDLE_LIMIT', '2'))
                area_codes = [item.strip() for item in os.environ.get('MEGABOX_AREA_CODES', '').split('||') if item.strip()]
                movies = [row for row in collector.build_movie_records(play_de) if row.get('movie_no')][:movie_limit]
                targets = []
                for area_code in area_codes:
                    for movie in movies:
                        movie_no = str(movie.get('movie_no') or '').strip()
                        if not movie_no:
                            continue
                        schedules = collector.build_schedule_records(movie_no=movie_no, play_de=play_de, area_cd=area_code)
                        if schedules:
                            targets.append({
                                'movie_no': movie_no,
                                'area_code': area_code,
                                'schedule_count': len(schedules),
                            })
                            break
                    if len(targets) >= bundle_limit:
                        break
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                    json.dumps({'targets': targets}, ensure_ascii=False),
                    encoding='utf-8'
                )
                """;
            case CGV -> """
                import json, os
                from pathlib import Path
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                    json.dumps({'targets': []}, ensure_ascii=False),
                    encoding='utf-8'
                )
                """;
        };
    }

    private static String buildSeatScript(CollectorProvider provider) {
        return switch (provider) {
            case CGV -> """
                import json, os
                from pathlib import Path
                from collectors.cgv.collector import CgvCollector
                collector = CgvCollector()
                summary = collector.summarize_seat_map(
                    site_no=os.environ['SITE_NO'],
                    scn_ymd=os.environ['SCN_YMD'],
                    scns_no=os.environ['SCNS_NO'],
                    scn_sseq=os.environ['SCN_SSEQ'],
                )
                seats = collector.build_seat_records(
                    site_no=os.environ['SITE_NO'],
                    scn_ymd=os.environ['SCN_YMD'],
                    scns_no=os.environ['SCNS_NO'],
                    scn_sseq=os.environ['SCN_SSEQ'],
                )
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                    json.dumps({'summary': summary, 'seats': seats}, ensure_ascii=False),
                    encoding='utf-8'
                )
                """;
            case LOTTE_CINEMA -> """
                import json, os
                from pathlib import Path
                from collectors.lotte.collector import LotteCinemaCollector
                collector = LotteCinemaCollector()
                cinema_id = int(os.environ['CINEMA_ID'])
                screen_id = int(os.environ['SCREEN_ID'])
                play_sequence = int(os.environ['PLAY_SEQUENCE'])
                screen_division_code = int(os.environ['SCREEN_DIVISION_CODE'])
                summary = collector.summarize_seat_map(
                    cinema_id=cinema_id,
                    screen_id=screen_id,
                    play_date=os.environ['PLAY_DATE'],
                    play_sequence=play_sequence,
                    screen_division_code=screen_division_code,
                )
                seats = collector.build_seat_records(
                    cinema_id=cinema_id,
                    screen_id=screen_id,
                    play_date=os.environ['PLAY_DATE'],
                    play_sequence=play_sequence,
                    screen_division_code=screen_division_code,
                )
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                    json.dumps({'summary': summary, 'seats': seats}, ensure_ascii=False),
                    encoding='utf-8'
                )
                """;
            case MEGABOX -> """
                import json, os
                from pathlib import Path
                from collectors.megabox.collector import MegaboxCollector
                collector = MegaboxCollector()
                summary = collector.summarize_seat_map(
                    play_schdl_no=os.environ['PLAY_SCHEDULE_NO'],
                    brch_no=os.environ['BRANCH_NO'],
                )
                seats = collector.build_seat_records(
                    play_schdl_no=os.environ['PLAY_SCHEDULE_NO'],
                    brch_no=os.environ['BRANCH_NO'],
                )
                Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                    json.dumps({'summary': summary, 'seats': seats}, ensure_ascii=False),
                    encoding='utf-8'
                )
                """;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
            .filter(Map.class::isInstance)
            .map(item -> (Map<String, Object>) item)
            .toList();
    }

    public record ProviderDiscoveryPayload(
        List<Map<String, Object>> targets
    ) {
    }
}

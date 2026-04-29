package kr.daboyeo.backend.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
                "PLAY_DATE", request.playDate().toString().replace("-", ""),
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

    public Map<String, Object> collectCgvSeatLayout(
        String siteNo,
        String screeningDate,
        String screenNo,
        String screenSequence,
        String seatAreaNo
    ) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("SITE_NO", siteNo);
        env.put("SCN_YMD", screeningDate);
        env.put("SCNS_NO", screenNo);
        env.put("SCN_SSEQ", screenSequence);
        if (seatAreaNo != null && !seatAreaNo.isBlank()) {
            env.put("SEAT_AREA_NO", seatAreaNo);
        }
        return executeJsonScript(buildCgvSeatLayoutScript(), env);
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

        try {
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
            if (stdout.isBlank()) {
                throw new IllegalStateException("Python collector returned no JSON payload.");
            }
            return objectMapper.readValue(stdout, MAP_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start python collector.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python collector wait was interrupted.", exception);
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
                from collectors.cgv.collector import CgvCollector
                bundle = CgvCollector().collect_bundle(
                    site_no=os.environ["SITE_NO"],
                    mov_no=os.environ["MOVIE_NO"],
                    scn_ymd=os.environ.get("PLAY_DATE") or None,
                    scns_no=None,
                    scn_sseq=None,
                )
                print(json.dumps(bundle, ensure_ascii=False))
                """;
            case LOTTE_CINEMA -> """
                import json, os
                from collectors.lotte.collector import LotteCinemaCollector
                bundle = LotteCinemaCollector().collect_bundle(
                    play_date=os.environ["PLAY_DATE"],
                    cinema_selector=os.environ["CINEMA_SELECTOR"],
                    representation_movie_code=os.environ["REPRESENTATION_MOVIE_CODE"],
                )
                print(json.dumps(bundle, ensure_ascii=False))
                """;
            case MEGABOX -> """
                import json, os
                from collectors.megabox.collector import MegaboxCollector
                bundle = MegaboxCollector().collect_bundle(
                    play_de=os.environ["PLAY_DATE"],
                    movie_no=os.environ["MOVIE_NO"],
                    area_cd=os.environ["AREA_CODE"],
                )
                print(json.dumps(bundle, ensure_ascii=False))
                """;
        };
    }

    private static String buildSeatScript(CollectorProvider provider) {
        return switch (provider) {
            case CGV -> """
                import json, os
                from collectors.cgv.collector import CgvCollector
                collector = CgvCollector()
                summary = collector.summarize_seat_map(
                    site_no=os.environ["SITE_NO"],
                    scn_ymd=os.environ["SCN_YMD"],
                    scns_no=os.environ["SCNS_NO"],
                    scn_sseq=os.environ["SCN_SSEQ"],
                )
                seats = collector.build_seat_records(
                    site_no=os.environ["SITE_NO"],
                    scn_ymd=os.environ["SCN_YMD"],
                    scns_no=os.environ["SCNS_NO"],
                    scn_sseq=os.environ["SCN_SSEQ"],
                )
                print(json.dumps({"summary": summary, "seats": seats}, ensure_ascii=False))
                """;
            case LOTTE_CINEMA -> """
                import json, os
                from collectors.lotte.collector import LotteCinemaCollector
                collector = LotteCinemaCollector()
                cinema_id = int(os.environ["CINEMA_ID"])
                screen_id = int(os.environ["SCREEN_ID"])
                play_sequence = int(os.environ["PLAY_SEQUENCE"])
                screen_division_code = int(os.environ["SCREEN_DIVISION_CODE"])
                summary = collector.summarize_seat_map(
                    cinema_id=cinema_id,
                    screen_id=screen_id,
                    play_date=os.environ["PLAY_DATE"],
                    play_sequence=play_sequence,
                    screen_division_code=screen_division_code,
                )
                seats = collector.build_seat_records(
                    cinema_id=cinema_id,
                    screen_id=screen_id,
                    play_date=os.environ["PLAY_DATE"],
                    play_sequence=play_sequence,
                    screen_division_code=screen_division_code,
                )
                print(json.dumps({"summary": summary, "seats": seats}, ensure_ascii=False))
                """;
            case MEGABOX -> """
                import json, os
                from collectors.megabox.collector import MegaboxCollector
                collector = MegaboxCollector()
                summary = collector.summarize_seat_map(
                    play_schdl_no=os.environ["PLAY_SCHEDULE_NO"],
                    brch_no=os.environ["BRANCH_NO"],
                )
                seats = collector.build_seat_records(
                    play_schdl_no=os.environ["PLAY_SCHEDULE_NO"],
                    brch_no=os.environ["BRANCH_NO"],
                )
                print(json.dumps({"summary": summary, "seats": seats}, ensure_ascii=False))
                """;
        };
    }

    private static String buildCgvSeatLayoutScript() {
        return """
            import json, os
            from collectors.cgv.collector import CgvCollector
            layout = CgvCollector().build_seat_layout(
                site_no=os.environ["SITE_NO"],
                scn_ymd=os.environ["SCN_YMD"],
                scns_no=os.environ["SCNS_NO"],
                scn_sseq=os.environ["SCN_SSEQ"],
                seat_area_no=os.environ.get("SEAT_AREA_NO", ""),
            )
            print(json.dumps(layout, ensure_ascii=False))
            """;
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
}

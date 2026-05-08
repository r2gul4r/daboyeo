package kr.daboyeo.backend.sync.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import org.junit.jupiter.api.Test;

class PythonCollectorBridgeTests {

    @Test
    void returnsOriginalPayloadWhenPythonWritesValidJson() {
        PythonCollectorBridge bridge = newBridge();

        Map<String, Object> payload = bridge.executeJsonScript(
            """
            import json, os
            from pathlib import Path
            print('collector progress log should go to stderr')
            Path(os.environ['OUTPUT_JSON_PATH']).write_text(
                json.dumps({'targets': [{'movie_no': '123'}]}, ensure_ascii=False),
                encoding='utf-8'
            )
            """,
            Map.of()
        );

        assertThat(payload).containsKey("targets");
        assertThat(payload).doesNotContainKey("success");
    }

    @Test
    void returnsFallbackPayloadWhenPythonWritesNothing() {
        PythonCollectorBridge bridge = newBridge();

        Map<String, Object> payload = bridge.executeJsonScript("pass", Map.of());

        assertThat(payload)
            .containsEntry("success", false)
            .containsEntry("data", java.util.List.of());
        assertThat(payload.get("error").toString()).contains("without writing JSON output");
    }

    @Test
    void returnsFallbackPayloadWhenPythonRaisesException() {
        PythonCollectorBridge bridge = newBridge();

        Map<String, Object> payload = bridge.executeJsonScript("raise RuntimeError('boom')", Map.of());

        assertThat(payload)
            .containsEntry("success", false)
            .containsEntry("data", java.util.List.of());
        assertThat(payload.get("error").toString()).contains("boom");
        assertThat(payload.get("stage")).isEqualTo("exception");
    }

    @Test
    void returnsFallbackPayloadWhenPythonWritesInvalidJson() {
        PythonCollectorBridge bridge = newBridge();

        Map<String, Object> payload = bridge.executeJsonScript(
            """
            import os
            from pathlib import Path
            Path(os.environ['OUTPUT_JSON_PATH']).write_text('not-json', encoding='utf-8')
            """,
            Map.of()
        );

        assertThat(payload)
            .containsEntry("success", false)
            .containsEntry("data", java.util.List.of());
        assertThat(payload.get("error").toString()).contains("invalid JSON");
        assertThat(payload.get("stage")).isEqualTo("invalid_json");
    }

    private static PythonCollectorBridge newBridge() {
        CollectorSyncProperties properties = new CollectorSyncProperties();
        properties.setPythonExecutable("python");
        properties.setProcessTimeoutSeconds(10);
        return new PythonCollectorBridge(properties, new ObjectMapper());
    }
}

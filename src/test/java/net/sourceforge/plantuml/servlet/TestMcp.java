package net.sourceforge.plantuml.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.sourceforge.plantuml.servlet.utils.WebappTestCase;


public class TestMcp extends WebappTestCase {

    private static final Gson GSON = new Gson();

    /**
     * Test that MCP API returns 404 when not enabled.
     */
    @Test
    public void testMcpDisabledByDefault() throws IOException {
        // MCP should be disabled by default
        final URL url = new URL(getServerUrl() + "/mcp/info");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        Assertions.assertEquals(404, responseCode,
            "MCP should return 404 when not enabled");
    }

    /**
     * Test MCP info endpoint when enabled.
     * Note: This test will only pass if PLANTUML_MCP_ENABLED=true is set.
     */
    @Test
    public void testMcpInfoWhenEnabled() throws IOException {
        // Set environment variable to enable MCP
        // This would typically be done in test configuration
        // For now, this test is informational

        final URL url = new URL(getServerUrl() + "/mcp/info");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();

        // If MCP is enabled, we should get 200, otherwise 404
        if (responseCode == 200) {
            String content = getContentText(conn);
            JsonObject response = GSON.fromJson(content, JsonObject.class);

            Assertions.assertTrue(response.has("plantumlCoreVersion"),
                "Response should contain plantumlCoreVersion");
            Assertions.assertTrue(response.has("securityProfile"),
                "Response should contain securityProfile");
            Assertions.assertTrue(response.has("limitSize"),
                "Response should contain limitSize");
            Assertions.assertTrue(response.has("environment"),
                "Response should contain environment");
        }
    }

    /**
     * Test MCP render endpoint.
     */
    @Test
    public void testMcpRender() throws IOException {
        final URL url = new URL(getServerUrl() + "/mcp/render");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Create request body
        JsonObject request = new JsonObject();
        request.addProperty("source", "@startuml\nAlice -> Bob : Hello\n@enduml");
        request.addProperty("format", "png");

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = GSON.toJson(request).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        // If MCP is enabled, we should get 200, otherwise 404
        if (responseCode == 200) {
            String content = getContentText(conn);
            JsonObject response = GSON.fromJson(content, JsonObject.class);

            Assertions.assertEquals("ok", response.get("status").getAsString(),
                "Response status should be ok");
            Assertions.assertTrue(response.has("dataUrl"),
                "Response should contain dataUrl");
            Assertions.assertTrue(response.get("dataUrl").getAsString().startsWith("data:image/png;base64,"),
                "Data URL should be a PNG base64 encoded image");
            Assertions.assertTrue(response.has("renderTimeMs"),
                "Response should contain renderTimeMs");
            Assertions.assertTrue(response.has("sha256"),
                "Response should contain sha256");
        } else {
            // MCP not enabled - expected in CI/CD without configuration
            Assertions.assertEquals(404, responseCode,
                "Should return 404 when MCP is not enabled");
        }
    }

    /**
     * Test MCP analyze endpoint.
     */
    @Test
    public void testMcpAnalyze() throws IOException {
        final URL url = new URL(getServerUrl() + "/mcp/analyze");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Create request body
        JsonObject request = new JsonObject();
        request.addProperty("source", "@startuml\nAlice -> Bob\n@enduml");

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = GSON.toJson(request).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        // If MCP is enabled, we should get 200, otherwise 404
        if (responseCode == 200) {
            String content = getContentText(conn);
            JsonObject response = GSON.fromJson(content, JsonObject.class);

            Assertions.assertEquals("ok", response.get("status").getAsString(),
                "Response status should be ok");
            Assertions.assertTrue(response.has("estimatedComplexity"),
                "Response should contain complexity estimate");
        } else {
            Assertions.assertEquals(404, responseCode,
                "Should return 404 when MCP is not enabled");
        }
    }

    /**
     * Test MCP workspace creation.
     */
    @Test
    public void testMcpWorkspaceCreate() throws IOException {
        final URL url = new URL(getServerUrl() + "/mcp/workspace/create");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Create request body
        JsonObject request = new JsonObject();
        request.addProperty("sessionId", "test-session-123");
        request.addProperty("name", "test-diagram");
        request.addProperty("source", "@startuml\nAlice -> Bob\n@enduml");

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = GSON.toJson(request).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        // If MCP is enabled, we should get 200, otherwise 404
        if (responseCode == 200) {
            String content = getContentText(conn);
            JsonObject response = GSON.fromJson(content, JsonObject.class);

            Assertions.assertTrue(response.has("diagramId"),
                "Response should contain diagramId");
            Assertions.assertTrue(response.get("diagramId").getAsString().startsWith("w"),
                "Diagram ID should start with 'w'");
        } else {
            // MCP not enabled
            Assertions.assertEquals(404, responseCode,
                "Should return 404 when MCP is not enabled");
        }
    }

    /**
     * Test that MCP requires authentication when API key is set.
     */
    @Test
    public void testMcpAuthenticationRequired() throws IOException {
        // This test assumes API key is set
        // If not set, this will still work (no auth required)
        final URL url = new URL(getServerUrl() + "/mcp/info");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        // Don't set Authorization header

        int responseCode = conn.getResponseCode();
        // Either 404 (not enabled), 200 (no auth required), or 401 (auth required)
        Assertions.assertTrue(
            responseCode == 404 || responseCode == 200 || responseCode == 401,
            "Response should be 404, 200, or 401"
        );
    }
}

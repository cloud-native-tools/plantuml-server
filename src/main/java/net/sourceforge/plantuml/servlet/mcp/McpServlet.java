/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * Project Info:  https://plantuml.com
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package net.sourceforge.plantuml.servlet.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import net.sourceforge.plantuml.mcp.McpService;
import net.sourceforge.plantuml.mcp.McpServiceImpl;
import net.sourceforge.plantuml.servlet.utility.Configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP (Model Context Protocol) servlet for PlantUML server.
 * Handles JSON-RPC 2.0 messages over HTTP POST.
 *
 * Configuration via environment variables / system properties:
 * - PLANTUML_MCP_ENABLED: "true" to enable the endpoint (default: false)
 * - PLANTUML_MCP_API_KEY: Optional API key for Bearer authentication
 *
 * Endpoint: POST /mcp
 * Content-Type: application/json
 *
 * Supported JSON-RPC methods:
 * - initialize: Establish MCP connection
 * - tools/list: List available tools
 * - tools/call: Execute a tool
 */
@SuppressWarnings("serial")
public class McpServlet extends HttpServlet {

    /** Exposed for unit tests. */
    public JsonRpcServer jsonRpcServer;

    private String apiKey;
    private boolean mcpEnabled;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();

        // Shared ObjectMapper for both JSON-RPC and HTTP responses
        this.objectMapper = new ObjectMapper();

        // Read configuration
        this.mcpEnabled = isMcpEnabled();
        this.apiKey = getConfigString("PLANTUML_MCP_API_KEY", "");

        if (mcpEnabled) {
            // Initialize JSON-RPC server with the MCP service implementation
            McpServiceImpl service = new McpServiceImpl(objectMapper);
            this.jsonRpcServer = new JsonRpcServer(objectMapper, service, McpService.class);

            log("MCP endpoint initialized" +
                (apiKey.isEmpty() ? " (no authentication)" : " (with API key authentication)"));
        } else {
            this.jsonRpcServer = null;
            log("MCP endpoint disabled (set PLANTUML_MCP_ENABLED=true to enable)");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        // Check if MCP is enabled
        if (!mcpEnabled || jsonRpcServer == null) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND,
                "MCP API is not enabled. Set PLANTUML_MCP_ENABLED=true to enable.");
            return;
        }

        // Authenticate if API key is configured
        if (!authenticate(request, response)) {
            return; // sendJsonError already called
        }

        // CORS + JSON response headers
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        try {
            // Delegate full JSON-RPC handling to jsonrpc4j (Jakarta Servlet API)
            jsonRpcServer.handle(request, response);
        } catch (Exception e) {
            log("Error handling JSON-RPC request: " + e.getMessage(), e);

            if (!response.isCommitted()) {
                sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        // CORS preflight
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        if (!mcpEnabled) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "MCP API is not enabled");
            return;
        }

        // Small informational endpoint about the MCP service
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "PlantUML MCP Server");
        info.put("version", "1.0.0");
        info.put("protocol", "JSON-RPC 2.0");
        info.put("transport", "HTTP POST");
        info.put("methods", new String[] { "initialize", "tools/list", "tools/call" });
        info.put("tools", new String[] { "diagram_type" });
        info.put("authentication", apiKey.isEmpty()
            ? "none"
            : "Bearer token required");

        writeJson(response, HttpServletResponse.SC_OK, info);
    }

    // ============= AUTHENTICATION =============

    /**
     * Authenticates the request using Bearer token if API key is configured.
     * Returns true if authentication succeeds or is not required.
     */
    private boolean authenticate(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        // No authentication required if no API key is set
        if (apiKey == null || apiKey.isEmpty()) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Missing or invalid Authorization header. Expected: Bearer <token>");
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (!apiKey.equals(token)) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return false;
        }

        return true;
    }

    // ============= CONFIGURATION =============

    private boolean isMcpEnabled() {
        String enabled = getConfigString("PLANTUML_MCP_ENABLED", "false");
        return "true".equalsIgnoreCase(enabled) || "1".equals(enabled);
    }

    private String getConfigString(String key, String defaultValue) {
        // Try Configuration utility first (checks system properties and environment variables)
        String value = Configuration.getString(key, null);
        if (value != null) {
            return value;
        }

        // Fallback to default value
        return defaultValue;
    }

    // ============= JSON HELPERS =============

    private void sendJsonError(HttpServletResponse response, int status, String message)
        throws IOException {

        Map<String, Object> errorBody = new LinkedHashMap<>();
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", status);
        error.put("message", message);
        errorBody.put("error", error);

        writeJson(response, status, errorBody);
    }

    private void writeJson(HttpServletResponse response, int status, Object body)
        throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Basic CORS header so clients can read error/info responses as well
        response.setHeader("Access-Control-Allow-Origin", "*");

        objectMapper.writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }
}

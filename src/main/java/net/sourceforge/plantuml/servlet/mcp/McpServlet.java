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

/**
 * MCP (Model Context Protocol) servlet for PlantUML server.
 * Handles JSON-RPC 2.0 messages over HTTP POST.
 *
 * Configuration via environment variables:
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

    public JsonRpcServer jsonRpcServer;
    private String apiKey;
    private boolean mcpEnabled;

    @Override
    public void init() throws ServletException {
        super.init();

        // Read configuration
        mcpEnabled = isMcpEnabled();
        apiKey = getConfigString("PLANTUML_MCP_API_KEY", "");

        if (mcpEnabled) {
            // Initialize JSON-RPC server
            ObjectMapper mapper = new ObjectMapper();
            McpServiceImpl service = new McpServiceImpl(mapper);
            this.jsonRpcServer = new JsonRpcServer(mapper, service, McpService.class);

            log("MCP endpoint initialized" +
                (apiKey.isEmpty() ? " (no authentication)" : " (with API key authentication)"));
        } else {
            log("MCP endpoint disabled (set PLANTUML_MCP_ENABLED=true to enable)");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check if MCP is enabled
        if (!mcpEnabled) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND,
                         "MCP API is not enabled. Set PLANTUML_MCP_ENABLED=true to enable.");
            return;
        }

        // Authenticate if API key is configured
        if (!authenticate(request, response)) {
            return;
        }

        // Set response headers
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        try {
            // Laisser jsonrpc4j gérer la requête Jakarta directement
            jsonRpcServer.handle(request, response);
        } catch (Exception e) {
            log("Error handling JSON-RPC request: " + e.getMessage(), e);
            sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error: " + e.getMessage());
        }    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Handle CORS preflight
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        if (!mcpEnabled) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // éventuellement : authenticate(request, response)

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println("PlantUML MCP endpoint is up.");
    }

    // ============= AUTHENTICATION =============

    /**
     * Authenticates the request using Bearer token if API key is configured.
     * Returns true if authentication succeeds or is not required.
     */
    private boolean authenticate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // No authentication required if no API key is set
        if (apiKey.isEmpty()) {
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

    // ============= ERROR HANDLING =============

    private void sendJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = "{\n" +
                "  \"error\": {\n" +
                "    \"code\": " + status + ",\n" +
                "    \"message\": \"" + escapeJson(message) + "\"\n" +
                "  }\n" +
                "}";

        response.getWriter().print(json);
        response.getWriter().flush();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

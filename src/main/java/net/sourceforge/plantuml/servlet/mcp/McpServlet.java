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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.security.SecurityUtils;
import net.sourceforge.plantuml.servlet.utility.Configuration;
import net.sourceforge.plantuml.version.Version;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP (Model Context Protocol) servlet for PlantUML server.
 * Provides a JSON API for AI agents to interact with PlantUML.
 */
@SuppressWarnings("SERIAL")
public class McpServlet extends HttpServlet {

    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isMcpEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "MCP API is not enabled");
            return;
        }

        if (!authenticate(request, response)) {
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            if (pathInfo.equals("/info")) {
                handleInfo(response);
            } else if (pathInfo.equals("/stats")) {
                handleStats(response);
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isMcpEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "MCP API is not enabled");
            return;
        }

        if (!authenticate(request, response)) {
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            JsonObject requestBody = readJsonRequest(request);

            if (pathInfo.equals("/render")) {
                handleRender(requestBody, response);
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            // Log error (servlet container will handle logging)
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error: " + e.getMessage());
        }
    }

    private boolean isMcpEnabled() {
        String enabled = Configuration.getString("PLANTUML_MCP_ENABLED", "false");
        return "true".equalsIgnoreCase(enabled);
    }

    private boolean authenticate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String apiKey = Configuration.getString("PLANTUML_MCP_API_KEY", "");
        if (apiKey.isEmpty()) {
            return true; // No API key required
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);
        if (!apiKey.equals(token)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return false;
        }

        return true;
    }

    private JsonObject readJsonRequest(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return GSON.fromJson(sb.toString(), JsonObject.class);
    }

    private void handleInfo(HttpServletResponse response) throws IOException {
        Map<String, Object> info = new HashMap<>();
        info.put("plantumlServerVersion", "0.0.1");
        info.put("plantumlLibraryVersion", Version.versionString());
        info.put("securityProfile", SecurityUtils.getSecurityProfile().toString());

        info.put("limitSize", Configuration.getInt("PLANTUML_LIMIT_SIZE", 4096));

        boolean statsEnabled = "on".equalsIgnoreCase(
            Configuration.getString("PLANTUML_STATS", "off")
        );
        info.put("statsEnabled", statsEnabled);

        Map<String, Object> environment = new HashMap<>();
        environment.put("backend", "jetty");
        environment.put("readOnly", !SecurityUtils.getSecurityProfile().toString().equals("UNSECURE"));
        info.put("environment", environment);

        sendJson(response, info);
    }

    private void handleStats(HttpServletResponse response) throws IOException {
        boolean statsEnabled = "on".equalsIgnoreCase(
            Configuration.getString("PLANTUML_STATS", "off")
        );
        if (!statsEnabled) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Stats not enabled");
            return;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "Stats endpoint not yet implemented");
        sendJson(response, stats);
    }


    private void handleRender(JsonObject requestBody, HttpServletResponse response)
            throws IOException {
        String source = getJsonString(requestBody, "source", null);
        if (source == null || source.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'source' field");
            return;
        }

        String format = getJsonString(requestBody, "format", "png");
        FileFormat fileFormat = parseFileFormat(format);

        long startTime = System.currentTimeMillis();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            SourceStringReader reader = new SourceStringReader(source);
            reader.outputImage(outputStream, 0, new FileFormatOption(fileFormat));

            byte[] imageBytes = outputStream.toByteArray();
            String dataUrl = formatDataUrl(imageBytes, fileFormat);
            String sha256 = computeSha256(imageBytes);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("format", format);
            result.put("dataUrl", dataUrl);
            result.put("renderTimeMs", System.currentTimeMillis() - startTime);
            result.put("sha256", sha256);

            sendJson(response, result);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                "Rendering failed: " + e.getMessage());
        }
    }


    private FileFormat parseFileFormat(String format) {
        switch (format.toLowerCase()) {
            case "svg": return FileFormat.SVG;
            case "png": return FileFormat.PNG;
            case "txt": return FileFormat.UTXT;
            case "eps": return FileFormat.EPS;
            case "pdf": return FileFormat.PDF;
            default: return FileFormat.PNG;
        }
    }

    private String formatDataUrl(byte[] data, FileFormat format) {
        String base64 = Base64.getEncoder().encodeToString(data);
        String mimeType = format.getMimeType();
        return "data:" + mimeType + ";base64," + base64;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getJsonString(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }

    private void sendJson(HttpServletResponse response, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.print(GSON.toJson(data));
        writer.flush();
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        PrintWriter writer = response.getWriter();
        writer.print(GSON.toJson(error));
        writer.flush();
    }
}

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
package net.sourceforge.plantuml.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the MCP service for PlantUML.
 * Currently supports one tool: diagram_type
 */
public class McpServiceImpl implements McpService {

    private final ObjectMapper objectMapper;
    private final List<ToolSchema> tools;

    public McpServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.tools = new ArrayList<>();
        this.tools.add(createDiagramTypeTool());
    }

    private ToolSchema createDiagramTypeTool() {
        ToolSchema tool = new ToolSchema();
        tool.name = "diagram_type";
        tool.description = "Detects the main PlantUML diagram type from the given source code. " +
                "Returns the diagram type (e.g., sequence, class, state, activity) and a confidence score.";

        try {
            String schemaJson = "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"required\": [\"source\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"source\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"description\": \"PlantUML diagram source code\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"additionalProperties\": false\n" +
                    "}";
            tool.inputSchema = objectMapper.readTree(schemaJson);
        } catch (Exception e) {
            tool.inputSchema = objectMapper.createObjectNode();
        }
        return tool;
    }

    // ============= INITIALIZE =============

    @Override
    public InitializeResult initialize(InitializeParams params) {
        InitializeResult result = new InitializeResult();

        // Support MCP protocol version 2025-06-18 or fallback to client's version
        result.protocolVersion = params.protocolVersion != null
                ? params.protocolVersion
                : "2025-06-18";

        // Server information
        ServerInfo info = new ServerInfo();
        info.name = "plantuml-mcp-server";
        info.version = "1.0.0";
        result.serverInfo = info;

        // Capabilities
        ServerCapabilities caps = new ServerCapabilities();
        ToolsServerCapabilities toolsCaps = new ToolsServerCapabilities();
        toolsCaps.listChanged = true;
        caps.tools = toolsCaps;
        result.capabilities = caps;

        return result;
    }

    // ============= TOOLS/LIST =============

    @Override
    public ToolsListResult listTools(ToolsListParams params) {
        ToolsListResult result = new ToolsListResult();
        result.tools = new ArrayList<>(tools);
        result.nextCursor = null; // No pagination for now
        return result;
    }

    // ============= TOOLS/CALL =============

    @Override
    public ToolsCallResult callTool(ToolsCallParams params) {
        ToolsCallResult result = new ToolsCallResult();
        result.content = new ArrayList<>();
        result.isError = false;

        // Dispatch to the appropriate tool handler
        switch (params.name) {
            case "diagram_type":
                handleDiagramType(params.arguments, result);
                break;
            default:
                handleUnknownTool(params.name, result);
        }

        return result;
    }

    private void handleDiagramType(JsonNode arguments, ToolsCallResult result) {
        try {
            // Extract source argument
            String source = "";
            if (arguments != null && arguments.has("source")) {
                source = arguments.get("source").asText("");
            }

            if (source.isEmpty()) {
                result.isError = true;
                addTextContent(result, "Error: 'source' argument is required and cannot be empty");
                return;
            }

            // Detect diagram type
            String diagramType = detectDiagramType(source);
            double confidence = computeConfidence(source, diagramType);

            // Build JSON response
            ObjectNode data = objectMapper.createObjectNode();
            data.put("diagramType", diagramType);
            data.put("confidence", confidence);
            data.put("source_length", source.length());

            ToolCallContent content = new ToolCallContent();
            content.type = "json";
            content.data = data;
            result.content.add(content);

        } catch (Exception e) {
            result.isError = true;
            addTextContent(result, "Error processing diagram_type: " + e.getMessage());
        }
    }

    private void handleUnknownTool(String toolName, ToolsCallResult result) {
        result.isError = true;
        addTextContent(result, "Unknown tool: " + toolName + ". Available tools: diagram_type");
    }

    private void addTextContent(ToolsCallResult result, String message) {
        ToolCallContent content = new ToolCallContent();
        content.type = "text";
        content.data = objectMapper.createObjectNode().put("message", message);
        result.content.add(content);
    }

    // ============= DIAGRAM TYPE DETECTION =============

    /**
     * Detects the PlantUML diagram type based on keywords and patterns.
     * This is a heuristic approach - can be replaced with actual PlantUML API calls.
     */
    private String detectDiagramType(String source) {
        if (source == null || source.isEmpty()) {
            return "unknown";
        }

        String lower = source.toLowerCase();

        // Check for explicit @start tags first (most reliable)
        if (lower.contains("@startmindmap")) return "mindmap";
        if (lower.contains("@startwbs")) return "wbs";
        if (lower.contains("@startgantt")) return "gantt";
        if (lower.contains("@startsalt")) return "salt";
        if (lower.contains("@startyaml")) return "yaml";
        if (lower.contains("@startjson")) return "json";
        if (lower.contains("@startebnf")) return "ebnf";
        if (lower.contains("@startregex")) return "regex";
        if (lower.contains("@startchen")) return "chen";

        // Sequence diagrams (common patterns)
        if (lower.contains("participant ") || 
            lower.contains("actor ") || 
            lower.contains("boundary ") ||
            lower.contains("control ") ||
            lower.contains("entity ") ||
            lower.contains("database ") ||
            lower.contains("collections ") ||
            (lower.contains("->") && !lower.contains("class "))) {
            return "sequence";
        }

        // Class diagrams
        if (lower.contains("class ") || 
            lower.contains("interface ") || 
            lower.contains("abstract ") ||
            lower.contains("enum ") ||
            lower.contains("extends ") ||
            lower.contains("implements ")) {
            return "class";
        }

        // State diagrams
        if (lower.contains("state ") || 
            lower.contains("[*]") ||
            lower.contains("-->") && lower.contains(":")) {
            return "state";
        }

        // Activity diagrams
        if (lower.contains("start") && lower.contains("stop") ||
            lower.contains("if (") ||
            lower.contains("while (") ||
            lower.contains("fork") ||
            lower.contains("split")) {
            return "activity";
        }

        // Component diagrams
        if (lower.contains("component ") || 
            lower.contains("package ") ||
            lower.contains("node ")) {
            return "component";
        }

        // Use case diagrams
        if (lower.contains("usecase ") || 
            (lower.contains("actor") && lower.contains("-->"))) {
            return "usecase";
        }

        // Object diagrams
        if (lower.contains("object ")) {
            return "object";
        }

        // Deployment diagrams
        if (lower.contains("artifact ") || lower.contains("cloud ")) {
            return "deployment";
        }

        // Timing diagrams
        if (lower.contains("robust ") || lower.contains("concise ")) {
            return "timing";
        }

        // Network diagrams
        if (lower.contains("nwdiag")) {
            return "network";
        }

        return "unknown";
    }

    /**
     * Computes a confidence score for the detected diagram type.
     * Based on the presence of specific keywords and patterns.
     */
    private double computeConfidence(String source, String diagramType) {
        if ("unknown".equals(diagramType)) {
            return 0.1;
        }

        String lower = source.toLowerCase();

        // High confidence if explicit @start tag is present
        if (lower.contains("@start" + diagramType)) {
            return 0.95;
        }

        // Medium-high confidence for strong keywords
        int keywordMatches = 0;
        int totalKeywords = 3;

        switch (diagramType) {
            case "sequence":
                if (lower.contains("participant")) keywordMatches++;
                if (lower.contains("->")) keywordMatches++;
                if (lower.contains("activate") || lower.contains("deactivate")) keywordMatches++;
                break;
            case "class":
                if (lower.contains("class ")) keywordMatches++;
                if (lower.contains("interface ") || lower.contains("abstract ")) keywordMatches++;
                if (lower.contains("extends") || lower.contains("implements")) keywordMatches++;
                break;
            case "state":
                if (lower.contains("state ")) keywordMatches++;
                if (lower.contains("[*]")) keywordMatches++;
                if (lower.contains("-->")) keywordMatches++;
                break;
            default:
                return 0.7; // Default medium confidence
        }

        return 0.5 + (keywordMatches / (double) totalKeywords) * 0.4;
    }
}

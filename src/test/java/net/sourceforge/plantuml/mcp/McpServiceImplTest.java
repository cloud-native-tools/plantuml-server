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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for McpServiceImpl.
 */
class McpServiceImplTest {

    private McpServiceImpl service;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        service = new McpServiceImpl(mapper);
    }

    @Test
    @DisplayName("Initialize should return server info and capabilities")
    void testInitialize() {
        // Arrange
        InitializeParams params = new InitializeParams();
        params.protocolVersion = "2025-06-18";
        params.clientInfo = new ClientInfo();
        params.clientInfo.name = "test-client";
        params.clientInfo.version = "1.0.0";

        // Act
        InitializeResult result = service.initialize(params);

        // Assert
        assertNotNull(result);
        assertEquals("2025-06-18", result.protocolVersion);
        assertNotNull(result.serverInfo);
        assertEquals("plantuml-mcp-server", result.serverInfo.name);
        assertEquals("1.0.0", result.serverInfo.version);
        assertNotNull(result.capabilities);
        assertNotNull(result.capabilities.tools);
        assertTrue(result.capabilities.tools.listChanged);
    }

    @Test
    @DisplayName("Initialize should fallback to default protocol version")
    void testInitializeWithNullProtocolVersion() {
        // Arrange
        InitializeParams params = new InitializeParams();
        params.protocolVersion = null;

        // Act
        InitializeResult result = service.initialize(params);

        // Assert
        assertEquals("2025-06-18", result.protocolVersion);
    }

    @Test
    @DisplayName("Tools list should contain diagram_type tool")
    void testListTools() {
        // Arrange
        ToolsListParams params = new ToolsListParams();

        // Act
        ToolsListResult result = service.listTools(params);

        // Assert
        assertNotNull(result);
        assertNotNull(result.tools);
        assertFalse(result.tools.isEmpty());
        assertEquals(1, result.tools.size());
        
        ToolSchema tool = result.tools.get(0);
        assertEquals("diagram_type", tool.name);
        assertNotNull(tool.description);
        assertNotNull(tool.inputSchema);
        assertTrue(tool.description.contains("diagram type"));
    }

    @Test
    @DisplayName("Call diagram_type with sequence diagram source")
    void testCallDiagramTypeSequence() {
        // Arrange
        String source = "@startuml\nparticipant User\nUser -> System: Request\n@enduml";
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("source", source);

        ToolsCallParams params = new ToolsCallParams();
        params.name = "diagram_type";
        params.arguments = arguments;

        // Act
        ToolsCallResult result = service.callTool(params);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError);
        assertNotNull(result.content);
        assertEquals(1, result.content.size());
        
        ToolCallContent content = result.content.get(0);
        assertEquals("json", content.type);
        JsonNode data = content.data;
        assertEquals("sequence", data.get("diagramType").asText());
        assertTrue(data.get("confidence").asDouble() > 0.5);
    }

    @Test
    @DisplayName("Call diagram_type with class diagram source")
    void testCallDiagramTypeClass() {
        // Arrange
        String source = "@startuml\nclass User {\n  +name: String\n}\n@enduml";
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("source", source);

        ToolsCallParams params = new ToolsCallParams();
        params.name = "diagram_type";
        params.arguments = arguments;

        // Act
        ToolsCallResult result = service.callTool(params);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError);
        assertEquals("class", result.content.get(0).data.get("diagramType").asText());
    }

    @Test
    @DisplayName("Call diagram_type with mindmap source")
    void testCallDiagramTypeMindmap() {
        // Arrange
        String source = "@startmindmap\n* Root\n** Branch\n@endmindmap";
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("source", source);

        ToolsCallParams params = new ToolsCallParams();
        params.name = "diagram_type";
        params.arguments = arguments;

        // Act
        ToolsCallResult result = service.callTool(params);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError);
        String diagramType = result.content.get(0).data.get("diagramType").asText();
        assertEquals("mindmap", diagramType);
        // Mindmap with @start tag should have high confidence
        assertTrue(result.content.get(0).data.get("confidence").asDouble() > 0.9);
    }

    @Test
    @DisplayName("Call diagram_type with empty source should return error")
    void testCallDiagramTypeEmptySource() {
        // Arrange
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("source", "");

        ToolsCallParams params = new ToolsCallParams();
        params.name = "diagram_type";
        params.arguments = arguments;

        // Act
        ToolsCallResult result = service.callTool(params);

        // Assert
        assertTrue(result.isError);
        assertEquals("text", result.content.get(0).type);
        assertTrue(result.content.get(0).data.get("message").asText().contains("required"));
    }

    @Test
    @DisplayName("Call diagram_type without source argument should return error")
    void testCallDiagramTypeNoSource() {
        // Arrange
        ObjectNode arguments = mapper.createObjectNode();

        ToolsCallParams params = new ToolsCallParams();
        params.name = "diagram_type";
        params.arguments = arguments;

        // Act
        ToolsCallResult result = service.callTool(params);

        // Assert
        assertTrue(result.isError);
    }

    @Test
    @DisplayName("Call unknown tool should return error")
    void testCallUnknownTool() {
        // Arrange
        ToolsCallParams params = new ToolsCallParams();
        params.name = "unknown_tool";
        params.arguments = mapper.createObjectNode();

        // Act
        ToolsCallResult result = service.callTool(params);

        // Assert
        assertTrue(result.isError);
        assertEquals("text", result.content.get(0).type);
        assertTrue(result.content.get(0).data.get("message").asText().contains("Unknown tool"));
    }

    @Test
    @DisplayName("Diagram type detection should handle various diagram types")
    void testDiagramTypeDetection() {
        assertDiagramType("@startuml\nstate Running\n@enduml", "state");
        assertDiagramType("@startuml\nstart\nstop\n@enduml", "activity");
        assertDiagramType("@startuml\ncomponent App\n@enduml", "component");
        assertDiagramType("@startuml\nusecase UC1\n@enduml", "usecase");
        assertDiagramType("@startuml\nobject obj1\n@enduml", "object");
        assertDiagramType("@startgantt\n[Task] lasts 1 day\n@endgantt", "gantt");
    }

    private void assertDiagramType(String source, String expectedType) {
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("source", source);

        ToolsCallParams params = new ToolsCallParams();
        params.name = "diagram_type";
        params.arguments = arguments;

        ToolsCallResult result = service.callTool(params);

        assertFalse(result.isError, "Should not return error for: " + expectedType);
        String actualType = result.content.get(0).data.get("diagramType").asText();
        assertEquals(expectedType, actualType, "Expected " + expectedType + " but got " + actualType);
    }
}

# MCP (Model Context Protocol) Implementation for PlantUML

## Overview

This implementation provides a JSON-RPC 2.0 compliant MCP server for PlantUML, enabling AI agents and other tools to interact with PlantUML diagrams programmatically.

## Architecture

- **Package**: `net.sourceforge.plantuml.mcp`
- **Transport**: HTTP POST with JSON-RPC 2.0
- **Protocol Version**: 2025-06-18
- **Library**: jsonrpc4j for JSON-RPC handling

## Configuration

Set these environment variables or system properties:

```bash
# Enable MCP endpoint
export PLANTUML_MCP_ENABLED=true

# Optional: Set API key for authentication
export PLANTUML_MCP_API_KEY="your-secret-key-here"
```

## Endpoints

### GET /mcp
Returns server information (no authentication required for this endpoint).

### POST /mcp
Handles JSON-RPC 2.0 requests for MCP methods.

## Supported Methods

### 1. initialize
Establishes an MCP connection.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-06-18",
    "clientInfo": {
      "name": "test-client",
      "version": "1.0.0"
    },
    "capabilities": {
      "tools": {
        "listChanged": true
      }
    }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2025-06-18",
    "serverInfo": {
      "name": "plantuml-mcp-server",
      "version": "1.0.0"
    },
    "capabilities": {
      "tools": {
        "listChanged": true
      }
    }
  }
}
```

### 2. tools/list
Lists available tools with their schemas.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {
    "cursor": null
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "diagram_type",
        "description": "Detects the main PlantUML diagram type from the given source code. Returns the diagram type (e.g., sequence, class, state, activity) and a confidence score.",
        "inputSchema": {
          "type": "object",
          "required": ["source"],
          "properties": {
            "source": {
              "type": "string",
              "description": "PlantUML diagram source code"
            }
          },
          "additionalProperties": false
        }
      }
    ],
    "nextCursor": null
  }
}
```

### 3. tools/call
Executes a tool with given arguments.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "diagram_type",
    "arguments": {
      "source": "@startuml\nparticipant User\nparticipant System\nUser -> System: Request\nSystem --> User: Response\n@enduml"
    }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "isError": false,
    "content": [
      {
        "type": "json",
        "data": {
          "diagramType": "sequence",
          "confidence": 0.9,
          "source_length": 95
        }
      }
    ]
  }
}
```

## Tools

### diagram_type

Detects the PlantUML diagram type using heuristic pattern matching.

**Supported Types:**
- sequence
- class
- state
- activity
- component
- usecase
- object
- deployment
- timing
- network
- mindmap
- wbs
- gantt
- salt
- yaml
- json
- ebnf
- regex
- chen

**Confidence Scores:**
- 0.95: Explicit @start tag present
- 0.7-0.9: Strong keyword matches
- 0.1: Unknown or no clear pattern

## Testing with curl

### 1. Check if MCP is enabled
```bash
curl http://localhost:8080/plantuml/mcp
```

### 2. Initialize connection (without auth)
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "clientInfo": {"name": "test", "version": "1.0"},
      "capabilities": {"tools": {"listChanged": true}}
    }
  }'
```

### 3. Initialize connection (with auth)
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "clientInfo": {"name": "test", "version": "1.0"},
      "capabilities": {"tools": {"listChanged": true}}
    }
  }'
```

### 4. List tools
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {"cursor": null}
  }'
```

### 5. Test diagram_type - Sequence diagram
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "diagram_type",
      "arguments": {
        "source": "@startuml\nparticipant User\nparticipant System\nUser -> System: Request\nSystem --> User: Response\n@enduml"
      }
    }
  }'
```

### 6. Test diagram_type - Class diagram
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "diagram_type",
      "arguments": {
        "source": "@startuml\nclass User {\n  +name: String\n  +email: String\n}\nclass Order {\n  +id: int\n}\nUser --> Order\n@enduml"
      }
    }
  }'
```

### 7. Test diagram_type - Mindmap
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "diagram_type",
      "arguments": {
        "source": "@startmindmap\n* Root\n** Branch 1\n*** Leaf 1\n** Branch 2\n@endmindmap"
      }
    }
  }'
```

### 8. Test error - Unknown tool
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "tools/call",
    "params": {
      "name": "unknown_tool",
      "arguments": {}
    }
  }'
```

### 9. Test error - Missing source
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key-here" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "tools/call",
    "params": {
      "name": "diagram_type",
      "arguments": {}
    }
  }'
```

## Development

### Adding a New Tool

1. Create the tool schema in `McpServiceImpl.createXxxTool()`
2. Add it to the `tools` list in constructor
3. Add a case in `callTool()` switch statement
4. Implement the handler method `handleXxx()`

Example:
```java
private ToolSchema createAnalyzeEntitiesTool() {
    ToolSchema tool = new ToolSchema();
    tool.name = "analyze_entities";
    tool.description = "Analyzes PlantUML diagram and extracts entities";
    
    String schemaJson = "{ ... }";
    tool.inputSchema = objectMapper.readTree(schemaJson);
    return tool;
}

private void handleAnalyzeEntities(JsonNode arguments, ToolsCallResult result) {
    // Implementation
}
```

### Integration with PlantUML Core

The current implementation uses heuristic pattern matching for diagram type detection. To integrate with PlantUML's actual parsing:

```java
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.Diagram;

private String detectDiagramTypeUsingPlantUML(String source) {
    try {
        SourceStringReader reader = new SourceStringReader(source);
        Diagram diagram = reader.getBlocks().get(0).getDiagram();
        return diagram.getDescription().getDescription(); // or similar
    } catch (Exception e) {
        return "unknown";
    }
}
```

## Security

- Authentication is optional but recommended for production
- Use Bearer token authentication with `PLANTUML_MCP_API_KEY`
- CORS is enabled by default (can be restricted if needed)
- Input validation is performed on all tool arguments

## Future Enhancements

- [ ] Add `analyze_entities` tool to extract classes, participants, etc.
- [ ] Add `validate_syntax` tool for syntax checking
- [ ] Add `render_preview` tool for generating thumbnails
- [ ] Implement pagination for tools/list
- [ ] Add rate limiting
- [ ] Add request logging and metrics
- [ ] Integrate with PlantUML's actual parsing API
- [ ] Support for batch operations

## References

- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [jsonrpc4j Documentation](https://github.com/briandilley/jsonrpc4j)

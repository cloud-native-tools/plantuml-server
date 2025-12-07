# Quick Start - MCP Implementation

## Prerequisites

- Java 11+
- Gradle 8.5+

## Build and Run

### 1. Build the project
```bash
./gradlew build
```

### 2. Run with Gretty (development)
```bash
# Enable MCP
export PLANTUML_MCP_ENABLED=true

# Optional: Set API key
export PLANTUML_MCP_API_KEY="your-secret-key"

# Run
./gradlew appRun
```

The server will start on http://localhost:8080/plantuml

### 3. Test the MCP endpoint

**Without authentication:**
```bash
curl http://localhost:8080/plantuml/mcp
```

**With authentication:**
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key" \
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

### 4. Run comprehensive tests

**Linux/Mac:**
```bash
chmod +x test-mcp.sh
./test-mcp.sh
```

**Windows:**
```powershell
.\test-mcp.ps1
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PLANTUML_MCP_ENABLED` | `false` | Enable/disable MCP endpoint |
| `PLANTUML_MCP_API_KEY` | (empty) | API key for Bearer authentication |

### System Properties (alternative)

```bash
./gradlew appRun -DPLANTUML_MCP_ENABLED=true -DPLANTUML_MCP_API_KEY=your-key
```

## Project Structure

```
src/main/java/net/sourceforge/plantuml/
├── mcp/                          # MCP protocol classes
│   ├── McpService.java           # JSON-RPC interface
│   ├── McpServiceImpl.java       # Implementation with tools
│   ├── InitializeParams.java     # Initialize method params
│   ├── InitializeResult.java     # Initialize method result
│   ├── ToolsListParams.java      # tools/list params
│   ├── ToolsListResult.java      # tools/list result
│   ├── ToolsCallParams.java      # tools/call params
│   ├── ToolsCallResult.java      # tools/call result
│   ├── ToolSchema.java           # Tool definition
│   ├── ToolCallContent.java      # Tool response content
│   ├── ClientInfo.java           # Client info
│   ├── ClientCapabilities.java   # Client capabilities
│   ├── ServerInfo.java           # Server info
│   ├── ServerCapabilities.java   # Server capabilities
│   ├── ToolsClientCapabilities.java
│   └── ToolsServerCapabilities.java
└── servlet/
    └── mcp/
        └── McpServlet.java       # HTTP servlet with JSON-RPC
```

## Available Tools

### diagram_type

Detects PlantUML diagram type from source code.

**Input:**
- `source` (string, required): PlantUML diagram source

**Output:**
- `diagramType` (string): Detected type (sequence, class, state, etc.)
- `confidence` (number): Confidence score (0.0-1.0)
- `source_length` (number): Length of source code

**Example:**
```bash
curl -X POST http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "diagram_type",
      "arguments": {
        "source": "@startuml\nclass Foo\n@enduml"
      }
    }
  }'
```

## Next Steps

See [MCP_IMPLEMENTATION.md](MCP_IMPLEMENTATION.md) for:
- Complete API documentation
- More test examples
- Development guide
- How to add new tools

## Troubleshooting

### MCP endpoint returns 404
- Check that `PLANTUML_MCP_ENABLED=true` is set
- Restart the server after changing environment variables

### Authentication fails
- Verify API key is set correctly
- Check the `Authorization: Bearer <token>` header format
- Try accessing without auth if no key is configured

### JSON-RPC errors
- Validate JSON syntax with a JSON validator
- Check that `jsonrpc`, `id`, `method`, and `params` are present
- Ensure `Content-Type: application/json` header is set

## Development

To add a new tool, edit `McpServiceImpl.java`:

1. Create tool schema in constructor
2. Add handler method
3. Update `callTool()` switch statement

See [MCP_IMPLEMENTATION.md](MCP_IMPLEMENTATION.md) for detailed examples.

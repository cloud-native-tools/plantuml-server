# MCP Implementation Changelog

## 2025-01-XX - MCP Protocol Implementation

### Added

#### Core MCP Protocol Support
- **Package**: `net.sourceforge.plantuml.mcp`
  - `McpService`: JSON-RPC 2.0 interface defining MCP methods
  - `McpServiceImpl`: Implementation with diagram type detection
  - Full set of MCP model classes (Initialize, Tools, etc.)

#### New MCP Servlet
- **File**: `src/main/java/net/sourceforge/plantuml/servlet/mcp/McpServlet.java`
  - JSON-RPC 2.0 over HTTP POST
  - Bearer token authentication support
  - CORS support for web clients
  - GET endpoint for server information

#### Tools Implemented
1. **diagram_type**
   - Detects PlantUML diagram type from source code
   - Supports 15+ diagram types (sequence, class, state, activity, etc.)
   - Returns confidence score (0.0-1.0)
   - Heuristic pattern matching algorithm

#### Configuration
- `PLANTUML_MCP_ENABLED`: Enable/disable MCP endpoint
- `PLANTUML_MCP_API_KEY`: Optional API key for authentication

#### Documentation
- `MCP_IMPLEMENTATION.md`: Complete implementation guide
- `MCP_QUICKSTART.md`: Quick start guide for developers
- Test scripts: `test-mcp.sh` (Linux/Mac) and `test-mcp.ps1` (Windows)

#### Tests
- `McpServiceImplTest`: JUnit 5 unit tests for MCP service
  - Initialize method tests
  - Tools list tests
  - Diagram type detection tests
  - Error handling tests

### Dependencies
- `jsonrpc4j:1.6.3`: JSON-RPC 2.0 library (already in build.gradle.kts)
- Jackson ObjectMapper: For JSON processing

### Technical Details

#### JSON-RPC Methods
1. **initialize**
   - Establishes MCP connection
   - Negotiates protocol version
   - Returns server capabilities

2. **tools/list**
   - Lists available tools with schemas
   - Supports pagination (cursor)

3. **tools/call**
   - Executes a tool with arguments
   - Returns structured results

#### Architecture
```
HTTP Request (JSON-RPC 2.0)
    ↓
McpServlet (Authentication)
    ↓
JsonRpcServer (jsonrpc4j)
    ↓
McpServiceImpl (Business Logic)
    ↓
Tool Handlers (diagram_type, etc.)
```

#### Security Features
- Optional Bearer token authentication
- Input validation on all arguments
- CORS headers for web client support
- Configurable via environment variables

### Files Created

```
src/main/java/net/sourceforge/plantuml/
├── mcp/
│   ├── ClientCapabilities.java
│   ├── ClientInfo.java
│   ├── InitializeParams.java
│   ├── InitializeResult.java
│   ├── McpService.java
│   ├── McpServiceImpl.java
│   ├── ServerCapabilities.java
│   ├── ServerInfo.java
│   ├── ToolCallContent.java
│   ├── ToolsCallParams.java
│   ├── ToolsCallResult.java
│   ├── ToolSchema.java
│   ├── ToolsClientCapabilities.java
│   ├── ToolsListParams.java
│   ├── ToolsListResult.java
│   └── ToolsServerCapabilities.java
└── servlet/mcp/
    └── McpServlet.java (replaced old version)

src/test/java/net/sourceforge/plantuml/mcp/
└── McpServiceImplTest.java

Documentation:
├── MCP_IMPLEMENTATION.md
├── MCP_QUICKSTART.md
├── test-mcp.sh
└── test-mcp.ps1
```

### Files Modified
- `src/main/java/net/sourceforge/plantuml/servlet/mcp/McpServlet.java` (completely rewritten)

### Breaking Changes
- Old MCP servlet implementation has been replaced
- Old endpoints `/mcp/info`, `/mcp/stats`, `/mcp/render` are removed
- New endpoint follows MCP protocol specification (JSON-RPC 2.0)

### Migration Guide
If you were using the old MCP endpoints:

**Old:**
```bash
curl http://localhost:8080/plantuml/mcp/render -d '{"source": "...", "format": "png"}'
```

**New:**
```bash
curl http://localhost:8080/plantuml/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "diagram_type",
      "arguments": {"source": "..."}
    }
  }'
```

### Next Steps
- [ ] Integrate with PlantUML's actual parsing API (replace heuristics)
- [ ] Add `analyze_entities` tool
- [ ] Add `validate_syntax` tool
- [ ] Add `render_preview` tool
- [ ] Implement rate limiting
- [ ] Add request logging and metrics
- [ ] Support for batch operations
- [ ] WebSocket transport for streaming

### References
- MCP Specification: https://spec.modelcontextprotocol.io/
- JSON-RPC 2.0: https://www.jsonrpc.org/specification
- jsonrpc4j: https://github.com/briandilley/jsonrpc4j

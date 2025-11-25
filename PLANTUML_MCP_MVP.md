# PlantUML MCP Server - MVP Specification

## Overview

This document defines the **Minimum Viable Product (MVP)** for the PlantUML MCP server.

We start with just 2 simple endpoints to validate the architecture before adding more complex features.

**Target branch:** `mcp1`

---

## MVP Endpoints

### 1. GET `/mcp/hello`

Simple health check endpoint to verify the MCP servlet is running.

#### Request
```
GET /mcp/hello
```

#### Response
```json
{
  "status": "ok",
  "message": "PlantUML MCP Server is running",
  "version": "1.0.0",
  "plantumlVersion": "1.2025.10"
}
```

**Purpose:** 
- Verify MCP servlet is properly configured
- Basic connectivity test
- Version information

---

### 2. GET `/mcp/examples/`

List and retrieve PlantUML example diagrams bundled with the server.

#### 2.1. List all examples

```
GET /mcp/examples/
```

**Response:**
```json
{
  "examples": [
    {
      "name": "sequence",
      "title": "Sequence Diagram Example",
      "description": "Basic sequence diagram with two participants"
    },
    {
      "name": "class",
      "title": "Class Diagram Example", 
      "description": "Simple class diagram with inheritance"
    },
    {
      "name": "usecase",
      "title": "Use Case Diagram Example",
      "description": "Use case diagram with actors and use cases"
    }
  ]
}
```

#### 2.2. Get specific example

```
GET /mcp/examples/sequence
```

**Response:**
```json
{
  "name": "sequence",
  "title": "Sequence Diagram Example",
  "description": "Basic sequence diagram with two participants",
  "source": "@startuml\nAlice -> Bob: Authentication Request\nBob --> Alice: Authentication Response\n@enduml"
}
```

**Purpose:**
- Provide AI assistants with working PlantUML examples
- Help users discover PlantUML capabilities
- Serve as templates for diagram generation

---

## Configuration

Only one new environment variable:

| Variable | Description | Default |
|----------|-------------|---------|
| `PLANTUML_MCP_ENABLED` | Enable MCP endpoints | `false` |

If `PLANTUML_MCP_ENABLED=false`, all `/mcp/*` and `/mcp/*` endpoints return `404`.

---

## Implementation Notes

### Servlet Mapping

Create `McpServlet.java` with two path mappings:
- `/mcp/*` → for main MCP endpoints
- `/mcp/*` → for special endpoints (examples, future extensions)

### Examples Source

Examples can be:
1. Hardcoded in the servlet (simplest for MVP)
2. Read from `src/main/resources/examples/` 
3. Read from existing `/WebUI/examples/` (if available)

**Recommendation for MVP:** Start with 3-5 hardcoded examples to keep it simple.

### Error Handling

Both endpoints should return proper HTTP status codes:
- `200` - Success
- `404` - Example not found / MCP disabled
- `500` - Internal server error

---

## Unit Tests

### Test 1: MCP Hello endpoint

```java
@Test
void helloEndpointReturnsOk() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/hello");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    assertEquals(200, resp.getStatus());
    String body = resp.getContentAsString();
    assertTrue(body.contains("\"status\":\"ok\""));
    assertTrue(body.contains("\"version\""));
}
```

### Test 2: Examples list endpoint

```java
@Test
void examplesListReturnsExamples() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/examples/");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    assertEquals(200, resp.getStatus());
    String body = resp.getContentAsString();
    assertTrue(body.contains("\"examples\""));
    assertTrue(body.contains("sequence"));
}
```

### Test 3: Get specific example

```java
@Test
void getExampleReturnsSource() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/examples/sequence");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    assertEquals(200, resp.getStatus());
    String body = resp.getContentAsString();
    assertTrue(body.contains("\"source\""));
    assertTrue(body.contains("@startuml"));
}
```

### Test 4: Unknown example returns 404

```java
@Test
void unknownExampleReturns404() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/examples/unknown");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    assertEquals(404, resp.getStatus());
}
```

### Test 5: MCP disabled returns 404

```java
@Test
void mcpDisabledReturns404() throws Exception {
    // Assuming MCP is disabled via config
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/hello");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    assertEquals(404, resp.getStatus());
}
```

---

## Success Criteria

The MVP is complete when:

1. ✅ `McpServlet` handles both `/mcp/*` and `/mcp/*` paths
2. ✅ `/mcp/hello` returns server info
3. ✅ `/mcp/examples/` lists 3-5 examples
4. ✅ `/mcp/examples/{name}` returns example source
5. ✅ All unit tests pass
6. ✅ MCP can be disabled via `PLANTUML_MCP_ENABLED`
7. ✅ No impact on existing endpoints

---

---

## 3. POST `/mcp/check`

Validate PlantUML source code and return syntax errors.

#### Request
```json
{
  "source": "@startuml\nAlice -> Bob: Hello\n@enduml"
}
```

#### Response (valid diagram)
```json
{
  "ok": true,
  "errors": []
}
```

#### Response (with errors)
```json
{
  "ok": false,
  "errors": [
    {
      "line": 2,
      "message": "Syntax error: unexpected token",
      "severity": "error"
    }
  ]
}
```

**Purpose:**
- Validate diagram syntax before rendering
- Provide detailed error messages to AI assistants
- Help users fix syntax issues

---

## 4. POST `/mcp/render`

Render a PlantUML diagram and return the result as base64-encoded PNG.

#### Request
```json
{
  "source": "@startuml\nAlice -> Bob: Hello\n@enduml",
  "format": "png"
}
```

#### Response
```json
{
  "ok": true,
  "format": "png",
  "dataBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
  "width": 200,
  "height": 150,
  "renderTimeMs": 45
}
```

#### Supported formats
- `png` (default)
- `svg`
- `txt`

**Purpose:**
- Allow AI assistants to generate and render diagrams
- Return rendered output directly in API response
- Support multiple output formats

**Implementation notes:**
- Must respect `PLANTUML_LIMIT_SIZE`
- Must use same rendering engine as existing endpoints
- Should reuse existing rendering logic from `/png`, `/svg` servlets

---

## 5. POST `/mcp/metadata`

Extract structural metadata from a PlantUML diagram.

#### Request
```json
{
  "source": "@startuml\nAlice -> Bob: Hello\nBob --> Alice: Hi\n@enduml"
}
```

#### Response
```json
{
  "ok": true,
  "diagramType": "sequence",
  "participants": ["Alice", "Bob"],
  "interactions": [
    {
      "from": "Alice",
      "to": "Bob",
      "message": "Hello",
      "type": "sync"
    },
    {
      "from": "Bob",
      "to": "Alice",
      "message": "Hi",
      "type": "return"
    }
  ],
  "directives": [],
  "complexity": {
    "lineCount": 4,
    "elementCount": 2
  }
}
```

**Purpose:**
- Extract diagram structure without rendering
- Allow AI to understand diagram content
- Enable diagram analysis and validation

**Implementation notes:**
- Parse PlantUML AST (Abstract Syntax Tree) if available
- Extract basic information: type, participants, relationships
- Keep response lightweight (no full AST dump)

---

## 6. Workspace Management (`/mcp/workspace/*`)

Manage ephemeral diagram workspaces for iterative editing.

### 6.1. POST `/mcp/workspace/create`

Create a new workspace.

#### Request
```json
{
  "sessionId": "user-abc-123"
}
```

#### Response
```json
{
  "ok": true,
  "workspaceId": "w-8f1b0341",
  "createdAt": "2025-11-25T10:30:00Z"
}
```

### 6.2. POST `/mcp/workspace/put`

Add or update a file in the workspace.

#### Request
```json
{
  "workspaceId": "w-8f1b0341",
  "filename": "sequence.puml",
  "content": "@startuml\nAlice -> Bob\n@enduml"
}
```

#### Response
```json
{
  "ok": true,
  "filename": "sequence.puml",
  "size": 35
}
```

### 6.3. POST `/mcp/workspace/get`

Retrieve a file from the workspace.

#### Request
```json
{
  "workspaceId": "w-8f1b0341",
  "filename": "sequence.puml"
}
```

#### Response
```json
{
  "ok": true,
  "filename": "sequence.puml",
  "content": "@startuml\nAlice -> Bob\n@enduml"
}
```

### 6.4. POST `/mcp/workspace/list`

List all files in a workspace.

#### Request
```json
{
  "workspaceId": "w-8f1b0341"
}
```

#### Response
```json
{
  "ok": true,
  "files": [
    {
      "filename": "sequence.puml",
      "size": 35,
      "updatedAt": "2025-11-25T10:30:00Z"
    },
    {
      "filename": "class.puml",
      "size": 128,
      "updatedAt": "2025-11-25T10:32:00Z"
    }
  ]
}
```

### 6.5. POST `/mcp/workspace/render`

Render a file from the workspace.

#### Request
```json
{
  "workspaceId": "w-8f1b0341",
  "filename": "sequence.puml",
  "format": "png"
}
```

#### Response
Same as `/mcp/render` response.

### 6.6. POST `/mcp/workspace/delete`

Delete a workspace and all its files.

#### Request
```json
{
  "workspaceId": "w-8f1b0341"
}
```

#### Response
```json
{
  "ok": true,
  "deletedFiles": 2
}
```

**Workspace Properties:**
- **In-memory only** - No persistence across server restarts
- **Bounded** - Max `PLANTUML_MCP_WORKSPACE_LIMIT` files per workspace (default: 20)
- **Ephemeral** - Automatically cleaned up after inactivity
- **Thread-safe** - Must support concurrent access

**Purpose:**
- Allow AI assistants to work with multiple related diagrams
- Support iterative editing workflow
- Enable file references between diagrams (includes)

---

## Additional Configuration

| Variable | Description | Default |
|----------|-------------|--------|
| `PLANTUML_MCP_ENABLED` | Enable MCP endpoints | `false` |
| `PLANTUML_MCP_WORKSPACE_LIMIT` | Max files per workspace | `20` |
| `PLANTUML_MCP_WORKSPACE_TTL` | Workspace TTL in seconds | `3600` |

---

## Additional Unit Tests

### Test 6: Check endpoint with valid diagram

```java
@Test
void checkEndpointReturnsOkForValidDiagram() throws Exception {
    String json = "{\"source\":\"@startuml\\nAlice -> Bob\\n@enduml\"}";
    
    MockHttpServletRequest req = postJson("/mcp/check", json);
    MockHttpServletResponse resp = new MockHttpServletResponse();
    
    servlet.service(req, resp);
    
    assertEquals(200, resp.getStatus());
    String body = resp.getContentAsString();
    assertTrue(body.contains("\"ok\":true"));
    assertTrue(body.contains("\"errors\":[]"));
}
```

### Test 7: Check endpoint with syntax error

```java
@Test
void checkEndpointReportsErrors() throws Exception {
    String json = "{\"source\":\"@startuml\\nThis is wrong\\n@enduml\"}";
    
    MockHttpServletResponse resp = call("/mcp/check", json);
    
    assertTrue(resp.getContentAsString().contains("\"ok\":false"));
    assertTrue(resp.getContentAsString().contains("errors"));
}
```

### Test 8: Render endpoint returns PNG base64

```java
@Test
void renderEndpointReturnsPngBase64() throws Exception {
    String json = "{\"source\":\"@startuml\\nAlice -> Bob\\n@enduml\"}";
    
    MockHttpServletResponse resp = call("/mcp/render", json);
    
    assertEquals(200, resp.getStatus());
    assertTrue(resp.getContentAsString().contains("\"format\":\"png\""));
    assertTrue(resp.getContentAsString().contains("\"dataBase64\""));
}
```

### Test 9: Metadata endpoint returns participants

```java
@Test
void metadataEndpointReturnsParticipants() throws Exception {
    String json = "{\"source\":\"@startuml\\nAlice -> Bob\\n@enduml\"}";
    
    MockHttpServletResponse resp = call("/mcp/metadata", json);
    
    assertEquals(200, resp.getStatus());
    assertTrue(resp.getContentAsString().contains("Alice"));
    assertTrue(resp.getContentAsString().contains("Bob"));
    assertTrue(resp.getContentAsString().contains("\"diagramType\":\"sequence\""));
}
```

### Test 10: Workspace lifecycle

```java
@Test
void workspaceLifecycle() throws Exception {
    // 1) Create workspace
    MockHttpServletResponse r1 = call("/mcp/workspace/create", 
        "{\"sessionId\":\"test-123\"}");
    String workspaceId = extractWorkspaceId(r1.getContentAsString());
    assertNotNull(workspaceId);
    
    // 2) Put file
    String putJson = String.format(
        "{\"workspaceId\":\"%s\",\"filename\":\"test.puml\",\"content\":\"@startuml\\nAlice->Bob\\n@enduml\"}",
        workspaceId);
    MockHttpServletResponse r2 = call("/mcp/workspace/put", putJson);
    assertEquals(200, r2.getStatus());
    
    // 3) Get file
    String getJson = String.format(
        "{\"workspaceId\":\"%s\",\"filename\":\"test.puml\"}",
        workspaceId);
    MockHttpServletResponse r3 = call("/mcp/workspace/get", getJson);
    assertTrue(r3.getContentAsString().contains("Alice->Bob"));
    
    // 4) List files
    String listJson = String.format("{\"workspaceId\":\"%s\"}", workspaceId);
    MockHttpServletResponse r4 = call("/mcp/workspace/list", listJson);
    assertTrue(r4.getContentAsString().contains("test.puml"));
    
    // 5) Render file
    String renderJson = String.format(
        "{\"workspaceId\":\"%s\",\"filename\":\"test.puml\",\"format\":\"png\"}",
        workspaceId);
    MockHttpServletResponse r5 = call("/mcp/workspace/render", renderJson);
    assertTrue(r5.getContentAsString().contains("\"dataBase64\""));
    
    // 6) Delete workspace
    String deleteJson = String.format("{\"workspaceId\":\"%s\"}", workspaceId);
    MockHttpServletResponse r6 = call("/mcp/workspace/delete", deleteJson);
    assertEquals(200, r6.getStatus());
}
```

### Test 11: Invalid JSON returns 400

```java
@Test
void invalidJsonReturns400() throws Exception {
    MockHttpServletResponse resp = call("/mcp/check", "{ invalid json }");
    assertEquals(400, resp.getStatus());
}
```

### Test 12: Workspace limit enforced

```java
@Test
void workspaceLimitEnforced() throws Exception {
    // Create workspace
    MockHttpServletResponse r1 = call("/mcp/workspace/create", 
        "{\"sessionId\":\"test-123\"}");
    String workspaceId = extractWorkspaceId(r1.getContentAsString());
    
    // Try to add more files than limit (assuming limit is 20)
    for (int i = 0; i < 25; i++) {
        String putJson = String.format(
            "{\"workspaceId\":\"%s\",\"filename\":\"file%d.puml\",\"content\":\"@startuml\\n@enduml\"}",
            workspaceId, i);
        MockHttpServletResponse resp = call("/mcp/workspace/put", putJson);
        
        if (i < 20) {
            assertEquals(200, resp.getStatus());
        } else {
            assertEquals(400, resp.getStatus());
            assertTrue(resp.getContentAsString().contains("limit"));
        }
    }
}
```

---

## Success Criteria

The MVP is complete when:

1. ✅ `McpServlet` handles all `/mcp/*` paths
2. ✅ `/mcp/hello` returns server info
3. ✅ `/mcp/examples/` lists examples and retrieves specific examples
4. ✅ `/mcp/check` validates PlantUML syntax
5. ✅ `/mcp/render` renders diagrams in PNG/SVG/TXT formats
6. ✅ `/mcp/metadata` extracts diagram structure
7. ✅ `/mcp/workspace/*` endpoints manage ephemeral workspaces
8. ✅ All unit tests pass
9. ✅ MCP can be disabled via `PLANTUML_MCP_ENABLED`
10. ✅ Workspace limits are enforced
11. ✅ No impact on existing endpoints
12. ✅ All security profiles are respected

---

## Implementation Priority

**Phase 1 (Core functionality):**
1. `/mcp/hello` - Health check
2. `/mcp/examples/*` - Example diagrams
3. `/mcp/check` - Syntax validation
4. `/mcp/render` - Diagram rendering

**Phase 2 (Advanced features):**
5. `/mcp/metadata` - Metadata extraction
6. `/mcp/workspace/*` - Workspace management

---

## Questions?

- Should examples include rendered previews (PNG base64)?
- Should we add a `/mcp/version` separate from `/mcp/hello`?
- Do we want CORS headers enabled for MCP endpoints?
- Should workspace cleanup be time-based or LRU?
- Do we need workspace authentication/authorization?

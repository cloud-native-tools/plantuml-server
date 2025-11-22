# MCP (Model Context Protocol) Server API

The PlantUML server now includes an optional MCP server that provides a JSON API for AI agents to interact with PlantUML in a structured, programmatic way.

## Configuration

The MCP server is disabled by default. Enable it using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PLANTUML_MCP_ENABLED` | Enable/disable MCP API | `false` |
| `PLANTUML_MCP_API_KEY` | Optional API key for authentication | (none) |
| `PLANTUML_MCP_WORKSPACE_LIMIT` | Max diagrams per workspace | `20` |
| `PLANTUML_MCP_MAX_REQUESTS_PER_MINUTE` | Rate limit (not yet implemented) | `0` |

### Example Configuration

```bash
export PLANTUML_MCP_ENABLED=true
export PLANTUML_MCP_API_KEY=my-secret-key
export PLANTUML_MCP_WORKSPACE_LIMIT=50
```

## Authentication

If `PLANTUML_MCP_API_KEY` is set, all requests must include an Authorization header:

```
Authorization: Bearer <API_KEY>
```

## API Endpoints

All endpoints use JSON for request and response bodies.

### GET /mcp/info

Get server information and capabilities.

**Response:**
```json
{
  "plantumlServerVersion": "2025.2",
  "plantumlCoreVersion": "v1.2025.10",
  "securityProfile": "INTERNET",
  "limitSize": 4096,
  "statsEnabled": false,
  "environment": {
    "backend": "jetty",
    "readOnly": true
  }
}
```

### POST /mcp/render

Render a PlantUML diagram.

**Request:**
```json
{
  "source": "@startuml\nAlice -> Bob : Hello\n@enduml",
  "format": "png"
}
```

**Response:**
```json
{
  "status": "ok",
  "format": "png",
  "dataUrl": "data:image/png;base64,...",
  "renderTimeMs": 42,
  "sha256": "..."
}
```

Supported formats: `png`, `svg`, `txt`, `eps`, `pdf`

### POST /mcp/render-url

Render from an encoded PlantUML URL.

**Request:**
```json
{
  "encodedUrl": "/svg/SoWkIImgAStDuU9o...",
  "format": "png"
}
```

**Response:** Same as `/mcp/render`

### POST /mcp/analyze

Analyze PlantUML source for errors and metadata.

**Request:**
```json
{
  "source": "@startuml\nAlice -> Bob\n@enduml"
}
```

**Response:**
```json
{
  "status": "ok",
  "messages": [],
  "includes": [],
  "diagramType": "unknown",
  "estimatedComplexity": {
    "lineCount": 3,
    "elementCount": 0
  }
}
```

### POST /mcp/workspace/create

Create a new diagram in a workspace.

**Request:**
```json
{
  "sessionId": "my-session",
  "name": "login-sequence",
  "source": "@startuml\nAlice -> Bob\n@enduml"
}
```

**Response:**
```json
{
  "diagramId": "w1"
}
```

### POST /mcp/workspace/update

Update an existing diagram.

**Request:**
```json
{
  "sessionId": "my-session",
  "diagramId": "w1",
  "source": "@startuml\nAlice -> Bob : Updated\n@enduml"
}
```

**Response:**
```json
{
  "status": "ok"
}
```

### POST /mcp/workspace/get

Get diagram source.

**Request:**
```json
{
  "sessionId": "my-session",
  "diagramId": "w1"
}
```

**Response:**
```json
{
  "diagramId": "w1",
  "source": "@startuml\nAlice -> Bob\n@enduml"
}
```

### POST /mcp/workspace/render

Render a workspace diagram.

**Request:**
```json
{
  "sessionId": "my-session",
  "diagramId": "w1",
  "format": "png"
}
```

**Response:** Same as `/mcp/render`

### POST /mcp/workspace/list

List all diagrams in a workspace.

**Request:**
```json
{
  "sessionId": "my-session"
}
```

**Response:**
```json
{
  "diagrams": [
    {
      "diagramId": "w1",
      "name": "login-sequence"
    }
  ]
}
```

### GET /mcp/stats

Get server statistics (requires `PLANTUML_STATS=on`).

**Response:**
```json
{
  "message": "Stats endpoint not yet implemented"
}
```

### GET /mcp/examples/list

List available examples.

**Response:**
```json
{
  "examples": [],
  "message": "Examples not yet implemented"
}
```

### GET /mcp/examples/get?name=example-name

Get example source.

**Response:**
```json
{
  "name": "example-name",
  "source": "",
  "message": "Example not found"
}
```

## Error Responses

All errors return appropriate HTTP status codes with JSON error messages:

```json
{
  "error": "Error message here"
}
```

Common status codes:
- `404` - MCP API not enabled or endpoint not found
- `400` - Bad request (missing required fields, invalid format)
- `401` - Unauthorized (invalid or missing API key)
- `500` - Internal server error

## Security

The MCP API respects all existing PlantUML security settings:
- `PLANTUML_SECURITY_PROFILE`
- `PLANTUML_LIMIT_SIZE`
- Allowlist rules
- File system access restrictions

The API does not expand PlantUML's permissions beyond what's already configured.

## Example Usage

### Using curl

```bash
# Enable MCP
export PLANTUML_MCP_ENABLED=true

# Render a diagram
curl -X POST http://localhost:8080/plantuml/mcp/render \
  -H "Content-Type: application/json" \
  -d '{
    "source": "@startuml\nAlice -> Bob : Hello\n@enduml",
    "format": "png"
  }'

# Create a workspace diagram
curl -X POST http://localhost:8080/plantuml/mcp/workspace/create \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session",
    "name": "my-diagram",
    "source": "@startuml\nAlice -> Bob\n@enduml"
  }'
```

### Using Python

```python
import requests
import json

# Server URL
base_url = "http://localhost:8080/plantuml/mcp"

# Render a diagram
response = requests.post(f"{base_url}/render", json={
    "source": "@startuml\nAlice -> Bob : Hello\n@enduml",
    "format": "png"
})

result = response.json()
print(f"Rendered in {result['renderTimeMs']}ms")
print(f"Data URL: {result['dataUrl'][:50]}...")
```

## Limitations

- Workspaces are in-memory only and not persisted
- Rate limiting is not yet implemented
- Examples integration is not yet implemented
- The analyze endpoint provides basic information only

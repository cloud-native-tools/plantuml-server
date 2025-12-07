# Test script for PlantUML MCP endpoint (PowerShell)

# Configuration
$MCP_URL = if ($env:MCP_URL) { $env:MCP_URL } else { "http://localhost:8080/plantuml/mcp" }
$API_KEY = $env:PLANTUML_MCP_API_KEY

# Function to make authenticated request
function Invoke-McpRequest {
    param(
        [string]$Method,
        [string]$Params,
        [int]$Id = 1
    )
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($API_KEY) {
        $headers["Authorization"] = "Bearer $API_KEY"
    }
    
    $requestBody = @{
        jsonrpc = "2.0"
        id = $Id
        method = $Method
        params = $Params | ConvertFrom-Json
    } | ConvertTo-Json -Depth 10
    
    Write-Host "`nRequest: $Method" -ForegroundColor Yellow
    $requestBody | ConvertFrom-Json | ConvertTo-Json -Depth 10 | Write-Host
    Write-Host ""
    
    try {
        $response = Invoke-RestMethod -Uri $MCP_URL -Method Post -Headers $headers -Body $requestBody
        Write-Host "Response:" -ForegroundColor Green
        $response | ConvertTo-Json -Depth 10 | Write-Host
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
        Write-Host $_.Exception.Response.StatusCode.value__ -ForegroundColor Red
    }
    
    Write-Host "`n-------------------------------------------`n"
}

# Test 1: Check server info (GET)
Write-Host "=== Test 1: Server Info (GET) ===" -ForegroundColor Yellow
try {
    $info = Invoke-RestMethod -Uri $MCP_URL -Method Get
    $info | ConvertTo-Json -Depth 10 | Write-Host
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}
Write-Host "`n-------------------------------------------`n"

# Test 2: Initialize
Write-Host "=== Test 2: Initialize ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "initialize" -Params @'
{
  "protocolVersion": "2025-06-18",
  "clientInfo": {
    "name": "test-powershell",
    "version": "1.0.0"
  },
  "capabilities": {
    "tools": {
      "listChanged": true
    }
  }
}
'@ -Id 1

# Test 3: List tools
Write-Host "=== Test 3: List Tools ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "tools/list" -Params '{"cursor": null}' -Id 2

# Test 4: Detect sequence diagram
Write-Host "=== Test 4: Detect Sequence Diagram ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "tools/call" -Params @'
{
  "name": "diagram_type",
  "arguments": {
    "source": "@startuml\nparticipant User\nparticipant System\nUser -> System: Request\nSystem --> User: Response\n@enduml"
  }
}
'@ -Id 3

# Test 5: Detect class diagram
Write-Host "=== Test 5: Detect Class Diagram ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "tools/call" -Params @'
{
  "name": "diagram_type",
  "arguments": {
    "source": "@startuml\nclass User {\n  +name: String\n  +email: String\n}\nclass Order {\n  +id: int\n}\nUser --> Order\n@enduml"
  }
}
'@ -Id 4

# Test 6: Detect mindmap
Write-Host "=== Test 6: Detect Mindmap ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "tools/call" -Params @'
{
  "name": "diagram_type",
  "arguments": {
    "source": "@startmindmap\n* Root\n** Branch 1\n*** Leaf 1\n** Branch 2\n@endmindmap"
  }
}
'@ -Id 5

# Test 7: Error - Unknown tool
Write-Host "=== Test 7: Error - Unknown Tool ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "tools/call" -Params @'
{
  "name": "unknown_tool",
  "arguments": {}
}
'@ -Id 6

# Test 8: Error - Missing source
Write-Host "=== Test 8: Error - Missing Source ===" -ForegroundColor Yellow
Invoke-McpRequest -Method "tools/call" -Params @'
{
  "name": "diagram_type",
  "arguments": {}
}
'@ -Id 7

Write-Host "`n=== All tests completed ===" -ForegroundColor Green

#!/bin/bash
# Test script for PlantUML MCP endpoint

# Configuration
MCP_URL="${MCP_URL:-http://localhost:8080/plantuml/mcp}"
API_KEY="${PLANTUML_MCP_API_KEY:-}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to make authenticated request
mcp_request() {
    local method="$1"
    local params="$2"
    local id="${3:-1}"
    
    local auth_header=""
    if [ -n "$API_KEY" ]; then
        auth_header="-H \"Authorization: Bearer $API_KEY\""
    fi
    
    local request_body=$(cat <<EOF
{
  "jsonrpc": "2.0",
  "id": $id,
  "method": "$method",
  "params": $params
}
EOF
)
    
    echo -e "${YELLOW}Request: $method${NC}"
    echo "$request_body" | jq .
    echo ""
    
    local response=$(eval curl -s -X POST "$MCP_URL" \
        -H "Content-Type: application/json" \
        $auth_header \
        -d "'$request_body'")
    
    echo -e "${GREEN}Response:${NC}"
    echo "$response" | jq .
    echo ""
    echo "-------------------------------------------"
    echo ""
}

# Test 1: Check server info (GET)
echo -e "${YELLOW}=== Test 1: Server Info (GET) ===${NC}"
curl -s "$MCP_URL" | jq .
echo ""
echo "-------------------------------------------"
echo ""

# Test 2: Initialize
echo -e "${YELLOW}=== Test 2: Initialize ===${NC}"
mcp_request "initialize" '{
  "protocolVersion": "2025-06-18",
  "clientInfo": {
    "name": "test-script",
    "version": "1.0.0"
  },
  "capabilities": {
    "tools": {
      "listChanged": true
    }
  }
}' 1

# Test 3: List tools
echo -e "${YELLOW}=== Test 3: List Tools ===${NC}"
mcp_request "tools/list" '{
  "cursor": null
}' 2

# Test 4: Detect sequence diagram
echo -e "${YELLOW}=== Test 4: Detect Sequence Diagram ===${NC}"
mcp_request "tools/call" '{
  "name": "diagram_type",
  "arguments": {
    "source": "@startuml\nparticipant User\nparticipant System\nUser -> System: Request\nSystem --> User: Response\n@enduml"
  }
}' 3

# Test 5: Detect class diagram
echo -e "${YELLOW}=== Test 5: Detect Class Diagram ===${NC}"
mcp_request "tools/call" '{
  "name": "diagram_type",
  "arguments": {
    "source": "@startuml\nclass User {\n  +name: String\n  +email: String\n}\nclass Order {\n  +id: int\n}\nUser --> Order\n@enduml"
  }
}' 4

# Test 6: Detect mindmap
echo -e "${YELLOW}=== Test 6: Detect Mindmap ===${NC}"
mcp_request "tools/call" '{
  "name": "diagram_type",
  "arguments": {
    "source": "@startmindmap\n* Root\n** Branch 1\n*** Leaf 1\n** Branch 2\n@endmindmap"
  }
}' 5

# Test 7: Error - Unknown tool
echo -e "${YELLOW}=== Test 7: Error - Unknown Tool ===${NC}"
mcp_request "tools/call" '{
  "name": "unknown_tool",
  "arguments": {}
}' 6

# Test 8: Error - Missing source
echo -e "${YELLOW}=== Test 8: Error - Missing Source ===${NC}"
mcp_request "tools/call" '{
  "name": "diagram_type",
  "arguments": {}
}' 7

echo -e "${GREEN}=== All tests completed ===${NC}"

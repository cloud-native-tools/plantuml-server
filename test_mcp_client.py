#!/usr/bin/env python3
"""
Simple MCP client for PlantUML server testing.

Usage:
    python test_mcp_client.py

Environment variables:
    MCP_URL: Server URL (default: http://localhost:8080/plantuml/mcp)
    PLANTUML_MCP_API_KEY: API key for authentication (optional)
"""

import json
import os
import sys
from typing import Any, Dict, Optional

try:
    import requests
except ImportError:
    print("Error: requests library not found. Install with: pip install requests")
    sys.exit(1)


class McpClient:
    """Simple MCP client for testing PlantUML MCP server."""

    def __init__(self, url: str, api_key: Optional[str] = None):
        self.url = url
        self.api_key = api_key
        self.request_id = 0

    def _make_request(self, method: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """Make a JSON-RPC 2.0 request."""
        self.request_id += 1

        headers = {
            "Content-Type": "application/json"
        }

        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        request_body = {
            "jsonrpc": "2.0",
            "id": self.request_id,
            "method": method,
            "params": params
        }

        print(f"\n{'='*70}")
        print(f"Request: {method}")
        print(f"{'='*70}")
        print(json.dumps(request_body, indent=2))

        response = requests.post(self.url, headers=headers, json=request_body)
        response.raise_for_status()

        result = response.json()
        print(f"\nResponse:")
        print(json.dumps(result, indent=2))

        return result

    def initialize(self, client_name: str = "python-test-client",
                   client_version: str = "1.0.0") -> Dict[str, Any]:
        """Initialize MCP connection."""
        params = {
            "protocolVersion": "2025-06-18",
            "clientInfo": {
                "name": client_name,
                "version": client_version
            },
            "capabilities": {
                "tools": {
                    "listChanged": True
                }
            }
        }
        return self._make_request("initialize", params)

    def list_tools(self) -> Dict[str, Any]:
        """List available tools."""
        params = {"cursor": None}
        return self._make_request("tools/list", params)

    def call_tool(self, tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        """Call a tool with arguments."""
        params = {
            "name": tool_name,
            "arguments": arguments
        }
        return self._make_request("tools/call", params)

    def detect_diagram_type(self, source: str) -> Dict[str, Any]:
        """Detect diagram type from PlantUML source."""
        return self.call_tool("diagram_type", {"source": source})


def test_sequence_diagram(client: McpClient):
    """Test sequence diagram detection."""
    print("\n" + "="*70)
    print("TEST: Sequence Diagram Detection")
    print("="*70)

    source = """@startuml
participant User
participant System
User -> System: Request
System --> User: Response
@enduml"""

    result = client.detect_diagram_type(source)

    if not result.get("result", {}).get("isError", True):
        data = result["result"]["content"][0]["data"]
        print(f"\n✅ Success!")
        print(f"   Diagram Type: {data['diagramType']}")
        print(f"   Confidence: {data['confidence']}")
    else:
        print("\n❌ Error!")
        print(f"   {result}")


def test_class_diagram(client: McpClient):
    """Test class diagram detection."""
    print("\n" + "="*70)
    print("TEST: Class Diagram Detection")
    print("="*70)

    source = """@startuml
class User {
  +name: String
  +email: String
  +login()
}
class Order {
  +id: int
  +total: float
  +calculate()
}
User --> Order
@enduml"""

    result = client.detect_diagram_type(source)

    if not result.get("result", {}).get("isError", True):
        data = result["result"]["content"][0]["data"]
        print(f"\n✅ Success!")
        print(f"   Diagram Type: {data['diagramType']}")
        print(f"   Confidence: {data['confidence']}")
    else:
        print("\n❌ Error!")
        print(f"   {result}")


def test_mindmap(client: McpClient):
    """Test mindmap detection."""
    print("\n" + "="*70)
    print("TEST: Mindmap Detection")
    print("="*70)

    source = """@startmindmap
* Root Concept
** Branch 1
*** Leaf 1.1
*** Leaf 1.2
** Branch 2
*** Leaf 2.1
@endmindmap"""

    result = client.detect_diagram_type(source)

    if not result.get("result", {}).get("isError", True):
        data = result["result"]["content"][0]["data"]
        print(f"\n✅ Success!")
        print(f"   Diagram Type: {data['diagramType']}")
        print(f"   Confidence: {data['confidence']}")
    else:
        print("\n❌ Error!")
        print(f"   {result}")


def test_error_handling(client: McpClient):
    """Test error handling."""
    print("\n" + "="*70)
    print("TEST: Error Handling - Empty Source")
    print("="*70)

    result = client.detect_diagram_type("")

    if result.get("result", {}).get("isError", False):
        print(f"\n✅ Success!")
        message = result["result"]["content"][0]["data"]["message"]
        print(f"   Message: {message}")
    else:
        print("\n❌ Should have returned an error!")


def main():
    """Main test function."""
    # Configuration
    mcp_url = os.getenv("MCP_URL", "http://localhost:8080/plantuml/mcp")
    api_key = os.getenv("PLANTUML_MCP_API_KEY")

    print("╔" + "="*70 + "╗")
    print("║" + " "*20 + "PlantUML MCP Client Test" + " "*26 + "║")
    print("╚" + "="*70 + "╝")
    print(f"\nServer: {mcp_url}")
    print(f"API Key: {'Set' if api_key else 'Not set'}")

    try:
        # Create client
        client = McpClient(mcp_url, api_key)

        # Test 1: Initialize
        print("\n" + "="*70)
        print("TEST: Initialize Connection")
        print("="*70)
        client.initialize()
        print("\n✅ Initialization successful!")

        # Test 2: List tools
        print("\n" + "="*70)
        print("TEST: List Available Tools")
        print("="*70)
        result = client.list_tools()
        tools = result.get("result", {}).get("tools", [])
        print(f"\n✅ Found {len(tools)} tool(s):")
        for tool in tools:
            print(f"   - {tool['name']}: {tool['description'][:60]}...")

        # Test 3-6: Various diagram types
        test_sequence_diagram(client)
        test_class_diagram(client)
        test_mindmap(client)
        test_error_handling(client)

        # Summary
        print("\n" + "="*70)
        print("🎉 All tests completed successfully!")
        print("="*70)

    except requests.exceptions.ConnectionError:
        print("\n❌ Error: Cannot connect to MCP server")
        print(f"   Make sure the server is running at {mcp_url}")
        print(f"   And PLANTUML_MCP_ENABLED=true is set")
        sys.exit(1)
    except requests.exceptions.HTTPError as e:
        print(f"\n❌ HTTP Error: {e}")
        print(f"   Response: {e.response.text}")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()

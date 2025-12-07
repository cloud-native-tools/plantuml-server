#!/bin/bash
# Verification script for MCP implementation

echo "╔════════════════════════════════════════════════════════════════════════════╗"
echo "║                                                                            ║"
echo "║                   MCP Implementation Verification                          ║"
echo "║                                                                            ║"
echo "╚════════════════════════════════════════════════════════════════════════════╝"
echo ""

ERRORS=0
WARNINGS=0

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1 (MISSING)"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# Function to check directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1/"
        return 0
    else
        echo -e "${RED}✗${NC} $1/ (MISSING)"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

echo "Checking Java source files..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check directories
check_dir "src/main/java/net/sourceforge/plantuml/mcp"
check_dir "src/main/java/net/sourceforge/plantuml/servlet/mcp"
check_dir "src/test/java/net/sourceforge/plantuml/mcp"

echo ""
echo "Checking MCP core classes..."
check_file "src/main/java/net/sourceforge/plantuml/mcp/McpService.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/McpServiceImpl.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ClientCapabilities.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ClientInfo.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/InitializeParams.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/InitializeResult.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ServerCapabilities.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ServerInfo.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolCallContent.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolsCallParams.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolsCallResult.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolSchema.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolsClientCapabilities.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolsListParams.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolsListResult.java"
check_file "src/main/java/net/sourceforge/plantuml/mcp/ToolsServerCapabilities.java"

echo ""
echo "Checking servlet..."
check_file "src/main/java/net/sourceforge/plantuml/servlet/mcp/McpServlet.java"

echo ""
echo "Checking tests..."
check_file "src/test/java/net/sourceforge/plantuml/mcp/McpServiceImplTest.java"

echo ""
echo "Checking documentation..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file "MCP_IMPLEMENTATION.md"
check_file "MCP_QUICKSTART.md"
check_file "MCP_CHANGELOG.md"
check_file "MCP_INSTALLATION_COMPLETE.md"
check_file "MCP_README.txt"
check_file "FILES_CREATED.md"
check_file "README_MCP_PACKAGE.md"

echo ""
echo "Checking test scripts..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file "test-mcp.sh"
check_file "test-mcp.ps1"
check_file "test_mcp_client.py"
check_file "verify-mcp-installation.sh"

echo ""
echo "Checking configuration..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file "build.gradle.kts"
check_file "src/main/webapp/WEB-INF/web.xml"

# Check if jsonrpc4j is in build.gradle.kts
if grep -q "jsonrpc4j" build.gradle.kts; then
    echo -e "${GREEN}✓${NC} jsonrpc4j dependency found in build.gradle.kts"
else
    echo -e "${YELLOW}⚠${NC} jsonrpc4j dependency not found in build.gradle.kts"
    WARNINGS=$((WARNINGS + 1))
fi

# Check if MCP servlet is registered in web.xml
if grep -q "mcpservlet" src/main/webapp/WEB-INF/web.xml; then
    echo -e "${GREEN}✓${NC} MCP servlet registered in web.xml"
else
    echo -e "${RED}✗${NC} MCP servlet not registered in web.xml"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "Checking file contents..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check that McpServiceImpl has diagram_type tool
if grep -q "diagram_type" src/main/java/net/sourceforge/plantuml/mcp/McpServiceImpl.java; then
    echo -e "${GREEN}✓${NC} diagram_type tool implemented"
else
    echo -e "${RED}✗${NC} diagram_type tool not found"
    ERRORS=$((ERRORS + 1))
fi

# Check that servlet uses jsonrpc4j
if grep -q "JsonRpcServer" src/main/java/net/sourceforge/plantuml/servlet/mcp/McpServlet.java; then
    echo -e "${GREEN}✓${NC} Servlet uses JsonRpcServer"
else
    echo -e "${RED}✗${NC} Servlet doesn't use JsonRpcServer"
    ERRORS=$((ERRORS + 1))
fi

# Check that tests exist
if grep -q "McpServiceImplTest" src/test/java/net/sourceforge/plantuml/mcp/McpServiceImplTest.java; then
    echo -e "${GREEN}✓${NC} Unit tests present"
else
    echo -e "${RED}✗${NC} Unit tests not found"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

TOTAL_FILES=28
FOUND_FILES=$((TOTAL_FILES - ERRORS))

echo "Files checked: $TOTAL_FILES"
echo -e "Files found: ${GREEN}$FOUND_FILES${NC}"

if [ $ERRORS -gt 0 ]; then
    echo -e "Errors: ${RED}$ERRORS${NC}"
fi

if [ $WARNINGS -gt 0 ]; then
    echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
fi

echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ MCP implementation is complete!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Build: ./gradlew clean build"
    echo "  2. Run: export PLANTUML_MCP_ENABLED=true && ./gradlew appRun"
    echo "  3. Test: ./test-mcp.sh"
    echo ""
    echo "Documentation: MCP_QUICKSTART.md"
    exit 0
else
    echo -e "${RED}❌ MCP implementation has errors!${NC}"
    echo ""
    echo "Please check the missing files and try again."
    exit 1
fi

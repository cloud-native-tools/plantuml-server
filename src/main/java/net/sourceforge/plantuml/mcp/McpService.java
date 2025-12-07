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
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

/**
 * MCP (Model Context Protocol) service interface.
 * Defines the JSON-RPC methods supported by the PlantUML MCP server.
 * 
 * Note: Using individual @JsonRpcParam for each field to match JSON-RPC params structure
 */
@JsonRpcService("/mcp")
public interface McpService {

    /**
     * Initialize the MCP connection.
     * This is the first method called when establishing a connection.
     */
    @JsonRpcMethod("initialize")
    InitializeResult initialize(
        @JsonRpcParam("protocolVersion") String protocolVersion,
        @JsonRpcParam("clientInfo") ClientInfo clientInfo,
        @JsonRpcParam("capabilities") ClientCapabilities capabilities
    );

    /**
     * List all available tools.
     * Returns the schema for each tool including name, description, and input schema.
     */
    @JsonRpcMethod("tools/list")
    ToolsListResult listTools(@JsonRpcParam("cursor") String cursor);

    /**
     * Call a specific tool with arguments.
     * Executes the tool and returns its result.
     */
    @JsonRpcMethod("tools/call")
    ToolsCallResult callTool(
        @JsonRpcParam("name") String name,
        @JsonRpcParam("arguments") JsonNode arguments
    );
}

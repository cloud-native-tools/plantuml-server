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
package net.sourceforge.plantuml.servlet.mcp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages in-memory workspaces for MCP sessions.
 * Each workspace can contain multiple diagrams.
 */
public class WorkspaceManager {

    private final Map<String, Workspace> workspaces = new ConcurrentHashMap<>();

    /**
     * Create a new diagram in a workspace.
     *
     * @param sessionId session identifier
     * @param name diagram name
     * @param source PlantUML source
     * @param limit maximum diagrams per workspace
     * @return diagram ID or null if limit exceeded
     */
    public String createDiagram(String sessionId, String name, String source, int limit) {
        Workspace workspace = workspaces.computeIfAbsent(sessionId, k -> new Workspace());

        if (workspace.getDiagramCount() >= limit) {
            return null;
        }

        return workspace.createDiagram(name, source);
    }

    /**
     * Update an existing diagram.
     *
     * @param sessionId session identifier
     * @param diagramId diagram identifier
     * @param source new PlantUML source
     * @return true if successful, false if diagram not found
     */
    public boolean updateDiagram(String sessionId, String diagramId, String source) {
        Workspace workspace = workspaces.get(sessionId);
        if (workspace == null) {
            return false;
        }
        return workspace.updateDiagram(diagramId, source);
    }

    /**
     * Get diagram source.
     *
     * @param sessionId session identifier
     * @param diagramId diagram identifier
     * @return diagram source or null if not found
     */
    public String getDiagram(String sessionId, String diagramId) {
        Workspace workspace = workspaces.get(sessionId);
        if (workspace == null) {
            return null;
        }
        return workspace.getDiagram(diagramId);
    }

    /**
     * List all diagrams in a workspace.
     *
     * @param sessionId session identifier
     * @return map of diagram IDs to names
     */
    public Map<String, String> listDiagrams(String sessionId) {
        Workspace workspace = workspaces.get(sessionId);
        if (workspace == null) {
            return new HashMap<>();
        }
        return workspace.listDiagrams();
    }

    /**
     * Inner class representing a workspace.
     */
    private static class Workspace {
        private final Map<String, Diagram> diagrams = new ConcurrentHashMap<>();
        private final AtomicInteger counter = new AtomicInteger(1);

        public String createDiagram(String name, String source) {
            String diagramId = "w" + counter.getAndIncrement();
            diagrams.put(diagramId, new Diagram(name, source));
            return diagramId;
        }

        public boolean updateDiagram(String diagramId, String source) {
            Diagram diagram = diagrams.get(diagramId);
            if (diagram == null) {
                return false;
            }
            diagram.setSource(source);
            return true;
        }

        public String getDiagram(String diagramId) {
            Diagram diagram = diagrams.get(diagramId);
            return diagram != null ? diagram.getSource() : null;
        }

        public Map<String, String> listDiagrams() {
            Map<String, String> result = new HashMap<>();
            diagrams.forEach((id, diagram) -> result.put(id, diagram.getName()));
            return result;
        }

        public int getDiagramCount() {
            return diagrams.size();
        }
    }

    /**
     * Inner class representing a diagram.
     */
    private static class Diagram {
        private final String name;
        private String source;

        Diagram(String diagramName, String diagramSource) {
            this.name = diagramName;
            this.source = diagramSource;
        }

        String getName() {
            return name;
        }

        String getSource() {
            return source;
        }

        void setSource(String newSource) {
            this.source = newSource;
        }
    }
}

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
package net.sourceforge.plantuml.servlet.utility;

import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;

/**
 * Shared PlantUML Server configuration.
 */
public final class Configuration {

    /**
     * Singleton configuration instance.
     */
    private static Configuration instance;
    /**
     * Configuration properties.
     */
    private Properties config;

    /**
     * Singleton constructor.
     */
    private Configuration() {
        config = new Properties();

        // Default values
        config.setProperty("SHOW_SOCIAL_BUTTONS", "off");
        config.setProperty("SHOW_GITHUB_RIBBON", "off");

        // MCP defaults
        config.setProperty("PLANTUML_MCP_ENABLED", getEnv("PLANTUML_MCP_ENABLED", "false"));
        config.setProperty("PLANTUML_MCP_API_KEY", getEnv("PLANTUML_MCP_API_KEY", ""));
        config.setProperty("PLANTUML_MCP_WORKSPACE_LIMIT", getEnv("PLANTUML_MCP_WORKSPACE_LIMIT", "20"));
        config.setProperty("PLANTUML_MCP_MAX_REQUESTS_PER_MINUTE", getEnv("PLANTUML_MCP_MAX_REQUESTS_PER_MINUTE", "0"));
        // End of default values

        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
            if (is != null) {
                config.load(is);
                is.close();
            }
        } catch (IOException e) {
            // Just log a warning
            e.printStackTrace();
        }
    }

    /**
     * Get the configuration.
     *
     * @return the complete configuration
     */
    public static Properties get() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance.config;
    }

    /**
     * Get a boolean configuration value.
     *
     * @param key config property key
     *
     * @return true if the value is "on"
     */
    public static boolean get(final String key) {
        if (get().getProperty(key) == null) {
            return false;
        }
        return get().getProperty(key).startsWith("on");
    }

    /**
     * Get a configuration value as string.
     *
     * @param key config property key
     * @param defaultValue default value if key not found
     *
     * @return configuration value or default
     */
    public static String getString(final String key, final String defaultValue) {
        String value = get().getProperty(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Get a configuration value as integer.
     *
     * @param key config property key
     * @param defaultValue default value if key not found
     *
     * @return configuration value or default
     */
    public static int getInt(final String key, final int defaultValue) {
        String value = get().getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Get environment variable helper.
     *
     * @param key environment variable name
     * @param defaultValue default value if not found
     *
     * @return environment variable value or default
     */
    private static String getEnv(final String key, final String defaultValue) {
        String value = System.getenv(key);
        return (value != null) ? value : defaultValue;
    }

}

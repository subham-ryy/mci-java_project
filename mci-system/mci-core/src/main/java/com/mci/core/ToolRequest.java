package com.mci.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable carrier representing an agent's request to perform an action.
 * This is the ONLY mechanism by which an agent requests external execution.
 *
 * @param runId     The ID of the overall run/session (identifies workspace)
 * @param subTaskId The ID of the specific subtask being executed
 * @param agentType The identity/type of the requesting agent
 * @param toolName  The action identifier (e.g., "file.read", "browser.fetch")
 * @param arguments Key-value arguments passed by the agent
 */
public record ToolRequest(
        String runId,
        String subTaskId,
        AgentType agentType,
        String toolName,
        Map<String, Object> arguments
) {
    public ToolRequest {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(agentType, "agentType must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        arguments = arguments == null ? Map.of() : Collections.unmodifiableMap(arguments);
    }

    /**
     * Helper to safely retrieve a string argument by key.
     */
    public String getString(String key) {
        Object val = arguments.get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * Helper to safely retrieve an integer argument with a fallback default.
     */
    public int getInt(String key, int defaultValue) {
        Object val = arguments.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}

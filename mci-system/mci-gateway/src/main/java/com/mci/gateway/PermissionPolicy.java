package com.mci.gateway;

import com.mci.core.AgentType;
import com.mci.core.Decision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides whether an agent is allowed to use a specific tool.
 *
 * Uses a strict allowlist — each agent type has a defined list
 * of tool names they are permitted to use. If it's not in the list, it's denied.
 *
 * Default is DENY (if no rule found, access is blocked).
 */
public class PermissionPolicy {

    // Map of agent type -> list of allowed tool names
    private final Map<AgentType, List<String>> allowlist;

    public PermissionPolicy() {
        allowlist = new HashMap<>();

        // NAVIGATOR agent can fetch web pages and save research sources
        allowlist.put(AgentType.NAVIGATOR, List.of("browser.fetch", "file.write"));

        // DATA_FILE agent can read/write data files and generate results.pdf
        allowlist.put(AgentType.DATA_FILE, List.of("file.read", "file.write", "pdf.generate"));

        // REPORT agent can read and write files (for executive report.md)
        allowlist.put(AgentType.REPORT, List.of("file.read", "file.write", "pdf.generate"));

        // DOCKER agent can run containers and read/write files
        allowlist.put(AgentType.DOCKER, List.of("docker.run", "file.read", "file.write"));
    }

    /**
     * Checks if an agent is allowed to use a tool.
     * Returns a Decision with ALLOW or DENY and a reason.
     */
    public Decision check(AgentType agentType, String toolName) {
        List<String> allowed = allowlist.get(agentType);

        if (allowed == null) {
            return Decision.deny("Unknown agent type: " + agentType);
        }

        if (allowed.contains(toolName)) {
            return Decision.allow(agentType + " is permitted to use " + toolName);
        }

        return Decision.deny(agentType + " is NOT allowed to use " + toolName);
    }
}

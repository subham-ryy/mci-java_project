package com.mci.gateway;

import com.mci.core.Decision;
import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import com.mci.persistence.AuditDao;
import com.mci.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The security chokepoint of the entire system.
 *
 * EVERY tool call goes through here:
 *   Agent → ToolGateway.execute(request) → Tool
 *
 * The gateway:
 *   1. Checks permissions (is this agent allowed to use this tool?)
 *   2. Logs the decision to the audit table BEFORE executing
 *   3. If allowed, runs the tool and logs the outcome
 *   4. If denied, returns a denial response
 *
 * This ensures no agent can ever bypass security.
 */
public class ToolGateway {

    private static final Logger log = LoggerFactory.getLogger(ToolGateway.class);

    private final PermissionPolicy policy;
    private final AuditDao auditDao;
    private final Map<String, Tool> tools;  // tool name -> tool instance
    private final java.util.List<ToolGatewayListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public ToolGateway(PermissionPolicy policy, AuditDao auditDao) {
        this.policy = policy;
        this.auditDao = auditDao;
        this.tools = new HashMap<>();
    }

    public void addListener(ToolGatewayListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ToolGatewayListener listener) {
        listeners.remove(listener);
    }

    /**
     * Register a tool so the gateway knows how to execute it.
     * Call this at startup for each tool (e.g., FileTool).
     */
    public void registerTool(String toolName, Tool tool) {
        tools.put(toolName, tool);
        log.info("Registered tool: {}", toolName);
    }

    /**
     * The main method — every agent action goes through here.
     *
     * @param request the agent's tool request
     * @return the tool's response, or a denial response
     */
    public ToolResponse execute(ToolRequest request) {
        String toolName = request.toolName();
        String agentType = request.agentType().name();

        // Step 1: Check permissions
        Decision decision = policy.check(request.agentType(), toolName);

        // Step 2: Log the decision BEFORE executing (audit trail)
        long auditId = auditDao.insertDecision(
                request.runId(),
                request.subTaskId(),
                agentType,
                toolName,
                request.arguments().toString(),
                decision.type().name(),
                decision.reason()
        );

        // Notify listeners of the gateway check
        for (ToolGatewayListener listener : listeners) {
            try {
                listener.onToolDecision(auditId, request.runId(), request.subTaskId(),
                        agentType, toolName, request.arguments().toString(),
                        decision.type().name(), decision.reason());
            } catch (Exception ignored) {}
        }

        // Step 3: If denied, stop here
        if (!decision.isAllowed()) {
            System.out.println("│    🔒 Gateway Check: ✗ DENIED (" + agentType + " → " + toolName + "): " + decision.reason());
            log.warn("DENIED: {} tried to use {} — {}", agentType, toolName, decision.reason());
            return ToolResponse.denied(decision.reason());
        }

        System.out.println("│    🔒 Gateway Check: ✓ ALLOWED (" + agentType + " → " + toolName + ")");

        // Step 4: Find the tool
        // Check exact tool name first (e.g. "file.write"), then prefix fallback ("file")
        Tool tool = tools.get(toolName);
        if (tool == null) {
            String toolPrefix = toolName.contains(".") ? toolName.split("\\.")[0] : toolName;
            tool = tools.get(toolPrefix);
        }

        if (tool == null) {
            log.error("Tool not found: {}", toolName);
            auditDao.updateOutcome(auditId, "ERROR");
            for (ToolGatewayListener listener : listeners) {
                try {
                    listener.onToolOutcome(auditId, request.runId(), "ERROR");
                } catch (Exception ignored) {}
            }
            return ToolResponse.error("Tool not found: " + toolName);
        }

        // Step 5: Execute the tool
        try {
            log.info("EXECUTING: {} → {} with args {}", agentType, toolName, request.arguments());
            ToolResponse response = tool.execute(request);

            // Step 6: Log the outcome
            String outcome = response.success() ? "SUCCESS" : "ERROR";
            auditDao.updateOutcome(auditId, outcome);
            for (ToolGatewayListener listener : listeners) {
                try {
                    listener.onToolOutcome(auditId, request.runId(), outcome);
                } catch (Exception ignored) {}
            }

            return response;

        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage());
            auditDao.updateOutcome(auditId, "ERROR");
            for (ToolGatewayListener listener : listeners) {
                try {
                    listener.onToolOutcome(auditId, request.runId(), "ERROR");
                } catch (Exception ignored) {}
            }
            return ToolResponse.error("Tool execution failed: " + e.getMessage());
        }
    }
}

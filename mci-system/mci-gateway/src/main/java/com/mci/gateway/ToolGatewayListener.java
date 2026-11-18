package com.mci.gateway;

/**
 * Listener interface for observing tool executions through ToolGateway.
 * Used for real-time streaming (SSE) and live monitoring.
 */
public interface ToolGatewayListener {

    /**
     * Called immediately after the gateway makes an ALLOW/DENY decision.
     */
    void onToolDecision(long auditId, String runId, String subtaskId, String agentType,
                        String toolName, String arguments, String decision, String reason);

    /**
     * Called after the tool finishes running (SUCCESS or ERROR).
     */
    default void onToolOutcome(long auditId, String runId, String outcome) {}
}

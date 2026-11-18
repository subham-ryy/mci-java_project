package com.mci.core;

/**
 * Represents the outcome of a security policy check performed by the ToolGateway.
 * Returned by PermissionPolicy and persisted to the audit log.
 *
 * @param type   ALLOW or DENY
 * @param reason Human-readable justification for the decision
 */
public record Decision(DecisionType type, String reason) {

    public enum DecisionType {
        ALLOW,
        DENY
    }

    public boolean isAllowed() {
        return type == DecisionType.ALLOW;
    }

    public static Decision allow(String reason) {
        return new Decision(DecisionType.ALLOW, reason);
    }

    public static Decision deny(String reason) {
        return new Decision(DecisionType.DENY, reason);
    }
}

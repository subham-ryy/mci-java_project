package com.mci.core;

/**
 * Immutable carrier representing the ToolGateway's response to an agent's request.
 *
 * @param success True if the tool executed successfully
 * @param denied  True if the request was blocked by the security policy
 * @param output  The successful text or serialized output of the tool
 * @param error   Error description or denial reason if not successful
 */
public record ToolResponse(
        boolean success,
        boolean denied,
        String output,
        String error
) {
    public static ToolResponse success(String output) {
        return new ToolResponse(true, false, output, null);
    }

    public static ToolResponse denied(String reason) {
        return new ToolResponse(false, true, null, reason);
    }

    public static ToolResponse error(String errorMessage) {
        return new ToolResponse(false, false, null, errorMessage);
    }
}

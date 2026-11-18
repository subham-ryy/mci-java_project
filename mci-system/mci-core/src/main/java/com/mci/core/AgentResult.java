package com.mci.core;

import java.util.Collections;
import java.util.List;

/**
 * Immutable carrier representing the final result of an agent executing a SubTask.
 *
 * @param success       Whether the subtask completed successfully
 * @param output        The textual output, summary, or report from the agent
 * @param filesProduced List of file paths/names created or modified in the workspace
 * @param error         Error message or failure reason if not successful
 */
public record AgentResult(
        boolean success,
        String output,
        List<String> filesProduced,
        String error
) {
    public AgentResult {
        filesProduced = filesProduced == null ? List.of() : Collections.unmodifiableList(filesProduced);
    }

    public static AgentResult success(String output) {
        return new AgentResult(true, output, List.of(), null);
    }

    public static AgentResult success(String output, List<String> filesProduced) {
        return new AgentResult(true, output, filesProduced, null);
    }

    public static AgentResult failure(String error) {
        return new AgentResult(false, null, List.of(), error);
    }
}

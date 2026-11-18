package com.mci.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents one step in the execution plan for a Task run.
 * Contains sequence order, agent assignment, dependencies, and execution state.
 *
 * @param id             Unique identifier for this subtask (e.g., "subtask-1")
 * @param runId          Identifier of the parent run/task
 * @param sequenceNumber Execution ordering or index
 * @param agentType      The specialized agent assigned to this step
 * @param description    The prompt or instructions for the agent
 * @param dependencyIds  List of SubTask IDs that must finish (DONE) before this subtask starts
 * @param status         Current lifecycle status
 * @param attempts       Number of times execution has been attempted
 * @param result         Final text output or summary if completed
 * @param error          Error message if failed
 */
public record SubTask(
        String id,
        String runId,
        int sequenceNumber,
        AgentType agentType,
        String description,
        List<String> dependencyIds,
        TaskStatus status,
        int attempts,
        String result,
        String error
) {
    public SubTask {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(agentType, "agentType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        dependencyIds = dependencyIds == null ? List.of() : Collections.unmodifiableList(dependencyIds);
    }

    /**
     * Factory for creating a brand new, pending subtask.
     */
    public static SubTask create(String id, String runId, int sequenceNumber, AgentType agentType, String description, List<String> dependencyIds) {
        return new SubTask(id, runId, sequenceNumber, agentType, description, dependencyIds, TaskStatus.PENDING, 0, null, null);
    }

    public SubTask withStatus(TaskStatus newStatus) {
        return new SubTask(id, runId, sequenceNumber, agentType, description, dependencyIds, newStatus, attempts, result, error);
    }

    public SubTask withResult(String newResult) {
        return new SubTask(id, runId, sequenceNumber, agentType, description, dependencyIds, TaskStatus.DONE, attempts, newResult, null);
    }

    public SubTask withError(String newError) {
        return new SubTask(id, runId, sequenceNumber, agentType, description, dependencyIds, TaskStatus.FAILED, attempts, result, newError);
    }

    public SubTask incrementAttempts() {
        return new SubTask(id, runId, sequenceNumber, agentType, description, dependencyIds, status, attempts + 1, result, error);
    }
}

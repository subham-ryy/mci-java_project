package com.mci.core;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an entire end-to-end user objective run.
 * Holds overall status, metadata, and the planned subtasks.
 *
 * @param id        Unique run identifier (e.g., "run-101" or UUID)
 * @param objective The user's original natural-language prompt or goal
 * @param status    Overall run lifecycle status
 * @param subTasks  The sequence or graph of subtasks for this run
 * @param createdAt Timestamp when this run was initiated
 */
public record Task(
        String id,
        String objective,
        TaskStatus status,
        List<SubTask> subTasks,
        Instant createdAt
) {
    public Task {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(objective, "objective must not be null");
        Objects.requireNonNull(status, "status must not be null");
        subTasks = subTasks == null ? List.of() : Collections.unmodifiableList(subTasks);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Factory for creating a newly initialized task.
     */
    public static Task create(String id, String objective) {
        return new Task(id, objective, TaskStatus.PENDING, List.of(), Instant.now());
    }

    public Task withStatus(TaskStatus newStatus) {
        return new Task(id, objective, newStatus, subTasks, createdAt);
    }

    public Task withSubTasks(List<SubTask> newSubTasks) {
        return new Task(id, objective, status, newSubTasks, createdAt);
    }
}

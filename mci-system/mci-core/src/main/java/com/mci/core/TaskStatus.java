package com.mci.core;

/**
 * Represents the lifecycle status of a task or subtask.
 * Stored as text in the persistence layer.
 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    DONE,
    FAILED,
    SKIPPED
}

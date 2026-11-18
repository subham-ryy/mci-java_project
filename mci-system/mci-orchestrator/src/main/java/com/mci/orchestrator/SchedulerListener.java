package com.mci.orchestrator;

import com.mci.core.SubTask;
import com.mci.core.TaskStatus;

/**
 * Listener interface for observing subtask status changes in Scheduler.
 * Used for real-time streaming (SSE) and progress monitoring.
 */
public interface SchedulerListener {

    /**
     * Called whenever a subtask status transitions (e.g. RUNNING, DONE, FAILED, SKIPPED).
     */
    void onSubTaskStatusChanged(SubTask subtask, TaskStatus newStatus);
}

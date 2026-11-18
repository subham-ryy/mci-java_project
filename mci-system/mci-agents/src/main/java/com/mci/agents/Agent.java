package com.mci.agents;

import com.mci.core.AgentResult;
import com.mci.core.SubTask;

/**
 * Interface for all agents in the system.
 * Each agent receives a subtask and returns a result.
 */
public interface Agent {

    /** Executes the given subtask and returns the result. */
    AgentResult execute(SubTask subTask);
}

package com.mci.orchestrator;

import com.mci.agents.Agent;
import com.mci.core.AgentResult;
import com.mci.core.SubTask;
import com.mci.core.TaskStatus;
import com.mci.persistence.SubTaskDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The Scheduler runs subtasks in order, respecting dependencies.
 *
 * It goes through each subtask sequentially:
 *   1. Check if all dependencies are DONE
 *   2. If yes, run the agent and save the result
 *   3. If a subtask fails, mark remaining dependents as SKIPPED
 */
public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

    private final SubTaskDao subTaskDao;
    private final Agent agent;
    private final java.util.List<SchedulerListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public Scheduler(SubTaskDao subTaskDao, Agent agent) {
        this.subTaskDao = subTaskDao;
        this.agent = agent;
    }

    public void addListener(SchedulerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(SchedulerListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(SubTask subtask, TaskStatus status) {
        for (SchedulerListener listener : listeners) {
            try {
                listener.onSubTaskStatusChanged(subtask, status);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Executes all subtasks for a run in sequence.
     *
     * @param subtasks the list of subtasks to execute (in order)
     */
    public void executeAll(List<SubTask> subtasks) {
        log.info("Scheduler starting — {} subtasks to execute", subtasks.size());
        System.out.println("┌── Executing Subtasks in Dependency Order ─────────────────");

        for (SubTask subtask : subtasks) {
            // Check if dependencies are met
            if (!areDependenciesMet(subtask, subtasks)) {
                log.warn("Skipping {} — dependencies not met", subtask.id());
                System.out.println("│  ⏭ " + subtask.id() + " [" + subtask.agentType() + "] SKIPPED (Dependencies not met)");
                SubTask skipped = subtask.withStatus(TaskStatus.SKIPPED);
                subTaskDao.update(skipped);
                notifyListeners(skipped, TaskStatus.SKIPPED);
                continue;
            }

            // Mark as RUNNING
            SubTask running = subtask.withStatus(TaskStatus.RUNNING).incrementAttempts();
            subTaskDao.update(running);
            notifyListeners(running, TaskStatus.RUNNING);

            System.out.println("│");
            System.out.println("│  ▶ Step " + subtask.sequenceNumber() + " [" + subtask.agentType() + " Agent]");
            System.out.println("│    Task: " + subtask.description());

            long start = System.currentTimeMillis();
            AgentResult result = agent.execute(subtask);
            long elapsed = System.currentTimeMillis() - start;

            // Save the result
            if (result.success()) {
                SubTask done = running.withResult(result.output());
                subTaskDao.update(done);
                notifyListeners(done, TaskStatus.DONE);

                // Extract summary line for clean presentation
                String[] outLines = result.output().split("\n");
                String preview = outLines[0].trim();
                if (preview.length() > 95) preview = preview.substring(0, 95) + "...";
                System.out.println("│    ↳ Result: " + preview);

                for (String l : outLines) {
                    l = l.trim();
                    if (l.startsWith("[") && l.endsWith("]") && (l.contains("Saved:") || l.contains("Output:") || l.contains("Fetched:"))) {
                        String clean = l.substring(1, l.length() - 1);
                        String icon = clean.toLowerCase().contains(".pdf") ? "📄 " :
                                      clean.toLowerCase().contains(".py") ? "⚡ " :
                                      clean.toLowerCase().contains("docker") ? "🐳 " :
                                      clean.toLowerCase().contains("url") ? "🌐 " : "📁 ";
                        System.out.println("│    " + icon + clean);
                    }
                }

                System.out.println("│    ✓ Step completed successfully in " + String.format("%.2fs", elapsed / 1000.0));
            } else {
                SubTask failed = running.withError(result.error());
                subTaskDao.update(failed);
                notifyListeners(failed, TaskStatus.FAILED);
                System.out.println("│    ✗ Failed: " + result.error());
            }
        }

        System.out.println("└── Execution Completed ──────────────────────────────────────\n");
    }

    /**
     * Checks if all dependencies of a subtask are completed (DONE).
     */
    private boolean areDependenciesMet(SubTask subtask, List<SubTask> allSubtasks) {
        for (String depId : subtask.dependencyIds()) {
            boolean found = false;
            for (SubTask other : allSubtasks) {
                if (other.id().equals(depId)) {
                    found = true;
                    SubTask latest = subTaskDao.findById(depId);
                    if (latest == null || latest.status() != TaskStatus.DONE) {
                        return false;
                    }
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}

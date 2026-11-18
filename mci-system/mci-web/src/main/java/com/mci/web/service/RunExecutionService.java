package com.mci.web.service;

import com.mci.core.SubTask;
import com.mci.core.Task;
import com.mci.core.TaskStatus;
import com.mci.gateway.ToolGateway;
import com.mci.gateway.ToolGatewayListener;
import com.mci.orchestrator.Planner;
import com.mci.orchestrator.Scheduler;
import com.mci.orchestrator.SchedulerListener;
import com.mci.persistence.ArtifactDao;
import com.mci.persistence.AuditDao;
import com.mci.persistence.RunDao;
import com.mci.persistence.SubTaskDao;
import com.mci.web.dto.ArtifactDto;
import com.mci.web.dto.AuditDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RunExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RunExecutionService.class);

    private final RunDao runDao;
    private final SubTaskDao subTaskDao;
    private final AuditDao auditDao;
    private final ArtifactDao artifactDao;
    private final ToolGateway toolGateway;
    private final Planner planner;
    private final Scheduler scheduler;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Set<String> knownArtifacts = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RunExecutionService(RunDao runDao,
                               SubTaskDao subTaskDao,
                               AuditDao auditDao,
                               ArtifactDao artifactDao,
                               ToolGateway toolGateway,
                               Planner planner,
                               Scheduler scheduler) {
        this.runDao = runDao;
        this.subTaskDao = subTaskDao;
        this.auditDao = auditDao;
        this.artifactDao = artifactDao;
        this.toolGateway = toolGateway;
        this.planner = planner;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void initListeners() {
        // Wire ToolGateway events to SSE
        toolGateway.addListener(new ToolGatewayListener() {
            @Override
            public void onToolDecision(long auditId, String runId, String subtaskId, String agentType,
                                       String toolName, String arguments, String decision, String reason) {
                String normalizedDecision = "ALLOW".equalsIgnoreCase(decision) || "ALLOWED".equalsIgnoreCase(decision)
                        ? "ALLOWED" : "DENIED";
                AuditDto dto = new AuditDto(
                        auditId,
                        runId,
                        agentType,
                        toolName,
                        arguments,
                        normalizedDecision,
                        reason,
                        "Pending execution",
                        Instant.now().toString()
                );
                sendSseEvent(runId, "tool_call", dto);
            }

            @Override
            public void onToolOutcome(long auditId, String runId, String outcome) {
                // Check if new artifacts were produced
                scanAndEmitArtifacts(runId);
            }
        });

        // Wire Scheduler events to SSE
        scheduler.addListener(new SchedulerListener() {
            @Override
            public void onSubTaskStatusChanged(SubTask subtask, TaskStatus newStatus) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("subtaskId", subtask.id());
                data.put("status", newStatus.name());
                data.put("agent", subtask.agentType().name());
                data.put("description", subtask.description());
                sendSseEvent(subtask.runId(), "subtask_status", data);

                scanAndEmitArtifacts(subtask.runId());
            }
        });
    }

    /**
     * Starts a new run asynchronously and returns the allocated run ID.
     */
    public synchronized String startRun(String objective) {
        int runCounter = runDao.findAll().size() + 1;
        String runId = "run-" + runCounter;
        while (runDao.findById(runId) != null) {
            runCounter++;
            runId = "run-" + runCounter;
        }

        Task task = Task.create(runId, objective);
        runDao.insert(task);
        runDao.updateStatus(runId, TaskStatus.RUNNING);

        new File(com.mci.web.config.MciConfig.resolveBaseDir("workspace"), runId).mkdirs();

        final String finalRunId = runId;
        executor.submit(() -> executeRun(finalRunId, objective));

        return runId;
    }

    private void executeRun(String runId, String objective) {
        try {
            log.info("Starting background execution for run: {}", runId);

            // Step 1: Plan subtasks
            List<SubTask> subtasks = planner.plan(runId, objective);
            for (SubTask st : subtasks) {
                subTaskDao.insert(st);
            }

            // Step 2: Execute subtasks via scheduler
            scheduler.executeAll(subtasks);

            // Step 3: Determine outcome
            List<SubTask> completed = subTaskDao.findByRunId(runId);
            boolean allDone = !completed.isEmpty() && completed.stream().allMatch(st -> st.status() == TaskStatus.DONE);

            runDao.updateStatus(runId, allDone ? TaskStatus.DONE : TaskStatus.FAILED);
            scanAndEmitArtifacts(runId);

            Map<String, String> statusEvent = Map.of("status", allDone ? "DONE" : "FAILED");
            sendSseEvent(runId, "run_status", statusEvent);

            log.info("Run {} finished with status {}", runId, allDone ? "DONE" : "FAILED");

        } catch (Exception e) {
            log.error("Execution failed for run {}: {}", runId, e.getMessage(), e);
            runDao.updateStatus(runId, TaskStatus.FAILED);
            sendSseEvent(runId, "run_status", Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }

    /**
     * Registers a new SSE emitter for a given runId.
     */
    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutes timeout

        emitters.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(runId, emitter));
        emitter.onTimeout(() -> removeEmitter(runId, emitter));
        emitter.onError(e -> removeEmitter(runId, emitter));

        try {
            // Send connection acknowledgment
            emitter.send(SseEmitter.event().name("connected").data(Map.of("runId", runId, "message", "SSE stream connected")));
        } catch (IOException e) {
            removeEmitter(runId, emitter);
        }

        return emitter;
    }

    private void removeEmitter(String runId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(runId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    public void sendSseEvent(String runId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(runId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        list.removeAll(deadEmitters);
    }

    private synchronized void scanAndEmitArtifacts(String runId) {
        File base = com.mci.web.config.MciConfig.resolveBaseDir("workspace");
        File dir = new File(base, runId);
        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File("./workspace/" + runId);
            if (!dir.exists() || !dir.isDirectory()) {
                dir = new File("../workspace/" + runId);
                if (!dir.exists() || !dir.isDirectory()) return;
            }
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isFile()) {
                String key = runId + ":" + f.getName();
                if (!knownArtifacts.contains(key)) {
                    knownArtifacts.add(key);
                    String kind = determineKind(f.getName());
                    String agent = determineAgentForFile(f.getName());
                    artifactDao.insert(runId, "workspace", f.getPath());

                    long artifactId = System.currentTimeMillis() % 100000;
                    ArtifactDto dto = new ArtifactDto(
                            artifactId,
                            runId,
                            agent,
                            "workspace/" + runId + "/" + f.getName(),
                            f.getName(),
                            kind,
                            f.length()
                    );
                    sendSseEvent(runId, "artifact_created", dto);
                }
            }
        }
    }

    public static String determineKind(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".svg")) {
            return "IMAGE";
        } else if (lower.endsWith(".py") || lower.endsWith(".js") || lower.endsWith(".sh") || lower.endsWith(".java")) {
            return "CODE";
        } else if (lower.endsWith(".pdf") || lower.endsWith(".md") || lower.endsWith(".txt")) {
            return "DOCUMENT";
        } else if (lower.endsWith(".json") || lower.endsWith(".csv") || lower.endsWith(".xml")) {
            return "DATA";
        }
        return "FILE";
    }

    public static String determineAgentForFile(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".py") || lower.contains("docker")) return "DOCKER";
        if (lower.endsWith(".md") || lower.contains("report")) return "REPORT";
        if (lower.endsWith(".pdf") || lower.endsWith(".json") || lower.endsWith(".csv")) return "DATA_FILE";
        return "DATA_FILE";
    }
}

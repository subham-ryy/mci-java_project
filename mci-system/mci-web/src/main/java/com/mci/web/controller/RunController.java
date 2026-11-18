package com.mci.web.controller;

import com.mci.core.SubTask;
import com.mci.core.Task;
import com.mci.core.TaskStatus;
import com.mci.persistence.ArtifactDao;
import com.mci.persistence.AuditDao;
import com.mci.persistence.RunDao;
import com.mci.persistence.SubTaskDao;
import com.mci.web.dto.*;
import com.mci.web.service.RunExecutionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RunController {

    private final RunDao runDao;
    private final SubTaskDao subTaskDao;
    private final AuditDao auditDao;
    private final ArtifactDao artifactDao;
    private final RunExecutionService runExecutionService;

    public RunController(RunDao runDao,
                         SubTaskDao subTaskDao,
                         AuditDao auditDao,
                         ArtifactDao artifactDao,
                         RunExecutionService runExecutionService) {
        this.runDao = runDao;
        this.subTaskDao = subTaskDao;
        this.auditDao = auditDao;
        this.artifactDao = artifactDao;
        this.runExecutionService = runExecutionService;
    }

    /**
     * POST /api/runs - Start a new run.
     */
    @PostMapping("/runs")
    public ResponseEntity<CreateRunResponse> createRun(@RequestBody CreateRunRequest request) {
        if (request == null || request.objective() == null || request.objective().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String runId = runExecutionService.startRun(request.objective().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateRunResponse(runId, runId));
    }

    /**
     * GET /api/runs - List runs, newest first.
     */
    @GetMapping("/runs")
    public ResponseEntity<List<RunDto>> listRuns() {
        List<Task> runs = runDao.findAll();
        List<RunDto> dtos = runs.stream().map(task -> {
            List<SubTaskDto> subtasks = subTaskDao.findByRunId(task.id()).stream()
                    .map(this::toSubTaskDto)
                    .collect(Collectors.toList());
            return new RunDto(
                    task.id(),
                    task.objective(),
                    task.status().name(),
                    task.createdAt().toString(),
                    subtasks
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/runs/{id} - Get run + its subtasks.
     */
    @GetMapping("/runs/{id}")
    public ResponseEntity<RunDto> getRun(@PathVariable("id") String id) {
        Task task = runDao.findById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        List<SubTaskDto> subtasks = subTaskDao.findByRunId(id).stream()
                .map(this::toSubTaskDto)
                .collect(Collectors.toList());

        RunDto dto = new RunDto(
                task.id(),
                task.objective(),
                task.status().name(),
                task.createdAt().toString(),
                subtasks
        );
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/runs/{id}/audit - Get audit log entries (supports filtering by decision and agent).
     */
    @GetMapping("/runs/{id}/audit")
    public ResponseEntity<List<AuditDto>> getAuditLog(@PathVariable("id") String id,
                                                      @RequestParam(value = "decision", required = false) String decisionFilter,
                                                      @RequestParam(value = "agent", required = false) String agentFilter) {
        List<AuditDao.AuditEntry> entries = auditDao.findByRunId(id);

        List<AuditDto> filtered = entries.stream()
                .filter(e -> {
                    if (decisionFilter != null && !decisionFilter.isBlank()) {
                        String dec = e.decision();
                        boolean matchAllow = ("ALLOW".equalsIgnoreCase(dec) || "ALLOWED".equalsIgnoreCase(dec))
                                && ("ALLOW".equalsIgnoreCase(decisionFilter) || "ALLOWED".equalsIgnoreCase(decisionFilter));
                        boolean matchDeny = ("DENY".equalsIgnoreCase(dec) || "DENIED".equalsIgnoreCase(dec))
                                && ("DENY".equalsIgnoreCase(decisionFilter) || "DENIED".equalsIgnoreCase(decisionFilter));
                        if (!matchAllow && !matchDeny) return false;
                    }
                    if (agentFilter != null && !agentFilter.isBlank()) {
                        if (!e.agentType().equalsIgnoreCase(agentFilter.trim())) return false;
                    }
                    return true;
                })
                .map(e -> {
                    String normDecision = ("ALLOW".equalsIgnoreCase(e.decision()) || "ALLOWED".equalsIgnoreCase(e.decision()))
                            ? "ALLOWED" : "DENIED";
                    return new AuditDto(
                            e.id(),
                            e.runId(),
                            e.agentType(),
                            e.toolName(),
                            e.arguments(),
                            normDecision,
                            e.reason(),
                            e.outcome() != null ? e.outcome() : "SUCCESS",
                            e.loggedAt()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    /**
     * GET /api/runs/{id}/artifacts - List artifacts generated during this run.
     */
    @GetMapping("/runs/{id}/artifacts")
    public ResponseEntity<List<ArtifactDto>> listArtifacts(@PathVariable("id") String id) {
        File dir = resolveRunDirectory(id);
        List<ArtifactDto> dtos = new ArrayList<>();

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                int index = 1;
                for (File f : files) {
                    if (f.isFile()) {
                        dtos.add(new ArtifactDto(
                                index++,
                                id,
                                RunExecutionService.determineAgentForFile(f.getName()),
                                "workspace/" + id + "/" + f.getName(),
                                f.getName(),
                                RunExecutionService.determineKind(f.getName()),
                                f.length()
                        ));
                    }
                }
            }
        }

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/runs/{id}/artifacts/{artifactId}/download - Download artifact by name or ID.
     */
    @GetMapping("/runs/{id}/artifacts/{artifactId}/download")
    public ResponseEntity<Resource> downloadArtifact(@PathVariable("id") String id,
                                                     @PathVariable("artifactId") String artifactId) {
        File targetFile = resolveArtifactFile(id, artifactId);
        if (targetFile == null || !targetFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(targetFile);
        String contentType = detectContentType(targetFile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + targetFile.getName() + "\"")
                .body(resource);
    }

    /**
     * GET /api/runs/{id}/artifacts/{artifactId}/view - View artifact directly in browser (inline).
     */
    @GetMapping("/runs/{id}/artifacts/{artifactId}/view")
    public ResponseEntity<Resource> viewArtifact(@PathVariable("id") String id,
                                                 @PathVariable("artifactId") String artifactId) {
        File targetFile = resolveArtifactFile(id, artifactId);
        if (targetFile == null || !targetFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(targetFile);
        String contentType = detectContentType(targetFile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + targetFile.getName() + "\"")
                .body(resource);
    }

    /**
     * GET /api/runs/{id}/artifacts/{artifactId}/content - Return text/json/markdown content.
     */
    @GetMapping("/runs/{id}/artifacts/{artifactId}/content")
    public ResponseEntity<FileContentDto> getArtifactContent(@PathVariable("id") String id,
                                                            @PathVariable("artifactId") String artifactId) {
        File targetFile = resolveArtifactFile(id, artifactId);
        if (targetFile == null || !targetFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        String name = targetFile.getName();
        String kind = RunExecutionService.determineKind(name);
        long size = targetFile.length();
        boolean isBinary = "IMAGE".equals(kind) || name.toLowerCase().endsWith(".pdf");

        String content = "";
        if (!isBinary) {
            try {
                content = Files.readString(targetFile.toPath());
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.ok(new FileContentDto(name, kind, size, content, isBinary));
    }

    private File resolveRunDirectory(String id) {
        File base = com.mci.web.config.MciConfig.resolveBaseDir("workspace");
        File dir = new File(base, id);
        if (dir.exists() && dir.isDirectory()) return dir;

        File f1 = new File("./workspace/" + id);
        if (f1.exists() && f1.isDirectory()) return f1;
        File f2 = new File("../workspace/" + id);
        if (f2.exists() && f2.isDirectory()) return f2;

        return dir;
    }

    private File resolveArtifactFile(String id, String artifactId) {
        File dir = resolveRunDirectory(id);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File exact = new File(dir, artifactId);
        if (exact.exists() && exact.isFile()) {
            return exact;
        }

        try {
            int targetIndex = Integer.parseInt(artifactId);
            File[] files = dir.listFiles(File::isFile);
            if (files != null && targetIndex >= 1 && targetIndex <= files.length) {
                return files[targetIndex - 1];
            }
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private String detectContentType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".md")) return "text/markdown;charset=UTF-8";
        if (name.endsWith(".txt") || name.endsWith(".py") || name.endsWith(".sh")) return "text/plain;charset=UTF-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";

        try {
            String probe = Files.probeContentType(file.toPath());
            if (probe != null) return probe;
        } catch (IOException ignored) {}

        return "application/octet-stream";
    }

    /**
     * GET /api/runs/{id}/report - Final report as { "markdown": string } (404 until ready).
     */
    @GetMapping("/runs/{id}/report")
    public ResponseEntity<ReportDto> getReport(@PathVariable("id") String id) {
        File reportFile = new File(resolveRunDirectory(id), "report.md");
        if (!reportFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            String content = Files.readString(reportFile.toPath());
            return ResponseEntity.ok(new ReportDto(content));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/runs/{id}/events - SSE live stream of events.
     */
    @GetMapping(value = "/runs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable("id") String id) {
        return runExecutionService.subscribe(id);
    }

    /**
     * GET /api/system/stats - KPI system stats.
     */
    @GetMapping("/system/stats")
    public ResponseEntity<SystemStatsDto> getSystemStats() {
        List<Task> runs = runDao.findAll();
        long resolved = runs.stream().filter(r -> r.status() == TaskStatus.DONE).count();
        int total = runs.size();
        String rate = total == 0 ? "100.0%" : String.format(Locale.US, "%.1f%%", (resolved * 100.0) / total);

        return ResponseEntity.ok(new SystemStatsDto(
                (int) resolved,
                4, // 4 active agents: Navigator, Docker, File/Data, Report
                rate,
                total
        ));
    }

    private SubTaskDto toSubTaskDto(SubTask st) {
        return new SubTaskDto(
                st.id(),
                st.sequenceNumber(),
                st.agentType().name(),
                st.description(),
                st.dependencyIds(),
                st.status().name(),
                st.attempts(),
                st.result(),
                st.error()
        );
    }
}

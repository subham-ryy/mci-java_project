package com.mci.app;

import com.mci.agents.LlmFactory;
import com.mci.agents.SimpleAgent;
import com.mci.core.SubTask;
import com.mci.core.Task;
import com.mci.core.TaskStatus;
import com.mci.gateway.PermissionPolicy;
import com.mci.gateway.ToolGateway;
import com.mci.orchestrator.Planner;
import com.mci.orchestrator.Scheduler;
import com.mci.persistence.*;
import com.mci.tools.FileTool;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Command-Line Interface for the Multi-Agent Computer Intelligence System.
 *
 * This is the main entry point. It:
 *   1. Loads the API key from the .env file
 *   2. Sets up the database, tools, gateway, and agents
 *   3. Takes user objectives from the command line
 *   4. Plans subtasks using Gemini AI
 *   5. Executes them through the secure gateway
 *   6. Shows results and audit log
 *
 * ──────────────────────────────────────────────────────
 * FUTURE VISION (not implemented yet):
 *
 * - REST API endpoints via Spring Boot (mci-web module):
 *     POST /api/runs          → start a new run
 *     GET  /api/runs/{id}     → get run status + subtasks
 *     GET  /api/runs          → list all runs
 *     GET  /api/audit/{runId} → view audit log
 *     SSE  /api/runs/{id}/stream → real-time progress updates
 *
 * - React/Next.js frontend for browser-based interaction
 * - WebSocket support for live agent activity monitoring
 * ──────────────────────────────────────────────────────
 */
public class Main {

    public static void main(String[] args) {
        // Ensure IPv4 is preferred for stable network connections
        System.setProperty("java.net.preferIPv4Stack", "true");

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  Multi-Agent Computer Intelligence System (MCI) ║");
        System.out.println("║  Version 1.0 — Command Line Interface           ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        // ── Step 1: Load API key from .env file ──
        Map<String, String> env = EnvLoader.load(".env");
        String apiKey = EnvLoader.get(env, "GEMINI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            System.out.println("[✓] API key found in .env file");
        } else {
            System.out.println("[i] No GEMINI_API_KEY found in .env (will use Built-in Agent Engine)");
        }

        // ── Step 2: Initialize database ──
        Database database = new Database("./data/mci.db");
        database.initSchema();
        RunDao runDao = new RunDao(database);
        SubTaskDao subTaskDao = new SubTaskDao(database);
        AuditDao auditDao = new AuditDao(database);
        System.out.println("[✓] SQLite database ready");

        // ── Step 3: Set up tools and gateway ──
        FileTool fileTool = new FileTool("./workspace");
        com.mci.tools.FileWriterTool fileWriterTool = new com.mci.tools.FileWriterTool("./workspace");
        com.mci.tools.PdfTool pdfTool = new com.mci.tools.PdfTool("./workspace");
        com.mci.tools.BrowserTool browserTool = new com.mci.tools.BrowserTool();
        com.mci.tools.DockerTool dockerTool = new com.mci.tools.DockerTool("./workspace");
        PermissionPolicy policy = new PermissionPolicy();
        ToolGateway gateway = new ToolGateway(policy, auditDao);
        gateway.registerTool("file", fileTool);
        gateway.registerTool("file.read", fileTool);
        gateway.registerTool("file.write", fileWriterTool);
        gateway.registerTool("pdf.generate", pdfTool);
        gateway.registerTool("pdf", pdfTool);
        gateway.registerTool("browser.fetch", browserTool);
        gateway.registerTool("browser", browserTool);
        gateway.registerTool("docker.run", dockerTool);
        gateway.registerTool("docker", dockerTool);
        System.out.println("[✓] Security gateway and tools ready (FileTool, FileWriterTool, PdfTool, BrowserTool, DockerTool)");

        // ── Step 4 & 5: Connect to Live Google Gemini AI (No Mock Mode) ──
        String configuredModel = EnvLoader.get(env, "GEMINI_MODEL");
        if (configuredModel == null || configuredModel.isBlank()) {
            configuredModel = "gemini-3.5-flash-lite"; // Fast, active, zero 503 errors
        }

        System.out.println("[...] Connecting to Live Google Gemini AI (" + configuredModel + ")...");
        ChatLanguageModel model;
        try {
            ChatLanguageModel liveModel = LlmFactory.createGemini(apiKey, configuredModel);
            String testResponse = liveModel.generate("Say exactly: 'MCI System Online!'");
            System.out.println("[✓] Live Gemini: " + testResponse.trim());
            System.out.println("[✓] Live Gemini AI verified and ready!\n");
            model = liveModel;
        } catch (Exception e) {
            System.out.println("[!] Connection to " + configuredModel + " encountered error: " + e.getMessage());
            // If primary model failed (e.g. 503 demand spike), try live gemini-3.5-flash-lite
            if (!"gemini-3.5-flash-lite".equals(configuredModel)) {
                System.out.println("[*] Attempting live failover to Google Gemini 3.5 Flash-Lite...");
                try {
                    ChatLanguageModel fallbackModel = LlmFactory.createGemini(apiKey, "gemini-3.5-flash-lite");
                    String testResponse = fallbackModel.generate("Say exactly: 'MCI System Online!'");
                    System.out.println("[✓] Live Gemini (gemini-3.5-flash-lite): " + testResponse.trim());
                    System.out.println("[✓] Live Gemini AI verified and ready!\n");
                    model = fallbackModel;
                } catch (Exception ex) {
                    System.err.println("[✗] Live Gemini AI connection failed: " + ex.getMessage());
                    return;
                }
            } else {
                System.err.println("[✗] Live Gemini AI connection failed: " + e.getMessage());
                return;
            }
        }

        // ── Step 6: Set up planner and multi-agent dispatcher ──
        Planner planner = new Planner(model);
        com.mci.agents.AgentDispatcher agent = new com.mci.agents.AgentDispatcher(model, gateway);
        Scheduler scheduler = new Scheduler(subTaskDao, agent);

        // ── Step 7: Interactive CLI loop ──
        Scanner scanner = new Scanner(System.in);
        int runCounter = runDao.findAll().size() + 1;
        String lastRunId = null;

        System.out.println("════════════════════════════════════════════");
        System.out.println("  Ready! Enter your objective below.");
        System.out.println("  Type 'quit' to exit, 'audit' to view log.");
        System.out.println("════════════════════════════════════════════");

        while (true) {
            System.out.print("\n> Your objective: ");
            String objective = scanner.nextLine().trim();

            if (objective.isEmpty()) continue;
            if (objective.equalsIgnoreCase("quit") || objective.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            // Show audit log for last run
            if (objective.equalsIgnoreCase("audit")) {
                if (lastRunId != null) {
                    showAuditLog(auditDao, lastRunId);
                } else {
                    System.out.println("[Audit] No runs executed yet in this session.");
                }
                continue;
            }

            // Create a new run with guaranteed unique ID
            String runId = "run-" + runCounter++;
            while (runDao.findById(runId) != null) {
                runId = "run-" + runCounter++;
            }
            lastRunId = runId;
            Task task = Task.create(runId, objective);
            runDao.insert(task);
            runDao.updateStatus(runId, TaskStatus.RUNNING);

            System.out.println("\n┌─ Run: " + runId + " ─────────────────────────────");
            System.out.println("│ Objective: " + objective);
            System.out.println("└──────────────────────────────────────────────");

            // Create workspace directory for this run
            new java.io.File("./workspace/" + runId).mkdirs();

            // Plan subtasks using Gemini
            System.out.println("\n[Planning] Asking Gemini to partition objective into subtasks...");
            List<SubTask> subtasks = planner.plan(runId, objective);

            System.out.println("[Plan] " + subtasks.size() + " subtasks created:");
            for (SubTask st : subtasks) {
                System.out.println("   " + st.sequenceNumber() + ". ["
                        + st.agentType() + "] " + st.description());
                subTaskDao.insert(st);
            }
            System.out.println();

            // Execute all subtasks
            scheduler.executeAll(subtasks);

            // Check results
            List<SubTask> completed = subTaskDao.findByRunId(runId);
            boolean allDone = completed.stream().allMatch(st -> st.status() == TaskStatus.DONE);
            runDao.updateStatus(runId, allDone ? TaskStatus.DONE : TaskStatus.FAILED);

            // Display generated files in the workspace run folder
            java.io.File runDir = new java.io.File("./workspace/" + runId);
            java.io.File[] files = runDir.listFiles();
            if (files != null && files.length > 0) {
                System.out.println("[Artifacts Saved in workspace/" + runId + "/]");
                for (java.io.File f : files) {
                    System.out.println("  • " + f.getName() + " (" + f.length() + " bytes)");
                }
            }

            System.out.println("\n[Run " + runId + " Status: " + (allDone ? "SUCCESS ✓" : "FAILED ✗") + "]");

            // Show audit summary
            showAuditLog(auditDao, runId);
        }

        scanner.close();
    }

    /**
     * Displays the audit log for a run — shows all gateway decisions.
     */
    private static void showAuditLog(AuditDao auditDao, String runId) {
        List<AuditDao.AuditEntry> audit = auditDao.findByRunId(runId);
        if (audit.isEmpty()) {
            System.out.println("[Security Audit] No gateway operations recorded for " + runId);
            return;
        }
        System.out.println("\n[Security Audit Trail: " + audit.size() + " gateway checks for " + runId + "]");
        for (AuditDao.AuditEntry entry : audit) {
            String icon = "ALLOW".equals(entry.decision()) ? "✓" : "✗";
            System.out.println("  " + icon + " " + entry.decision()
                    + " | Agent: " + String.format("%-10s", entry.agentType())
                    + " → Tool: " + String.format("%-12s", entry.toolName())
                    + " | " + entry.reason());
        }
    }
}

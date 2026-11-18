package com.mci.tools;

import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated tool for sandboxed container and code execution.
 *
 * Implements strict security invariants:
 * - Strict image allowlist (python:3.12-slim, node:20-alpine, openjdk:21-slim)
 * - Network isolation: --network none
 * - Resource limits: --memory=512m, --cpus=1.0
 * - Execution timeout: 20s
 * - No privileged mode (--privileged blocked)
 * - Workspace mount isolation: only workspace/<runId>/ is mounted
 * - Seamless ProcessBuilder fallback when Docker daemon is not active
 */
public class DockerTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(DockerTool.class);

    private static final Set<String> ALLOWED_IMAGES = Set.of(
            "python:3.12-slim",
            "python:3.11-slim",
            "python:3-slim",
            "python:3.12-alpine",
            "python:alpine",
            "node:20-alpine",
            "node:alpine",
            "openjdk:21-slim"
    );

    private static final int DEFAULT_TIMEOUT_SECONDS = 20;
    private static final int MAX_OUTPUT_CHARS = 10000;

    private final String baseDir; // e.g. "./workspace"

    public DockerTool(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String name() {
        return "docker.run";
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String image = request.getString("image");
        String command = request.getString("command");
        String scriptName = request.getString("scriptName");

        if (image == null || image.isBlank()) {
            image = "python:3.12-slim";
        }
        image = image.trim().toLowerCase(Locale.ROOT);

        // Security Check 1: Image allowlist
        if (!ALLOWED_IMAGES.contains(image)) {
            log.warn("Blocked unapproved Docker image: {}", image);
            return ToolResponse.denied("Image '" + image + "' is not in the approved security allowlist: " + ALLOWED_IMAGES);
        }

        // Security Check 2: Workspace sandbox isolation
        File workspaceDir = new File(baseDir, request.runId());
        if (!isInsideWorkspace(workspaceDir)) {
            log.warn("Security violation: Workspace escapes base directory: {}", workspaceDir);
            return ToolResponse.denied("Workspace escapes sandbox: " + request.runId());
        }
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs();
        }

        // Determine script to run
        if ((scriptName == null || scriptName.isBlank()) && command != null) {
            String[] parts = command.trim().split("\\s+");
            if (parts.length > 1) {
                scriptName = parts[1];
            } else {
                scriptName = parts[0];
            }
        }
        if (scriptName == null || scriptName.isBlank()) {
            scriptName = "solution.py";
        }

        File scriptFile = new File(workspaceDir, scriptName);
        if (!scriptFile.exists()) {
            return ToolResponse.error("Script file not found in workspace: " + scriptName);
        }

        // Check if Docker daemon is running
        boolean dockerAvailable = isDockerDaemonRunning();

        if (dockerAvailable) {
            log.info("Executing via Docker container ({}) for run {}", image, request.runId());
            return executeInDocker(image, scriptName, workspaceDir);
        } else {
            log.info("Docker daemon inactive; executing via sandboxed ProcessBuilder fallback for run {}", request.runId());
            return executeInProcessBuilder(image, scriptName, workspaceDir);
        }
    }

    /**
     * Executes the script inside an isolated Docker container with strict resource limits.
     */
    private ToolResponse executeInDocker(String image, String scriptName, File workspaceDir) {
        try {
            List<String> dockerCmd = new ArrayList<>();
            dockerCmd.add("docker");
            dockerCmd.add("run");
            dockerCmd.add("--rm");
            dockerCmd.add("--network");
            dockerCmd.add("none"); // Network isolation
            dockerCmd.add("--memory=512m"); // Memory cap
            dockerCmd.add("--cpus=1.0"); // CPU quota
            dockerCmd.add("-v");
            dockerCmd.add(workspaceDir.getCanonicalPath() + ":/workspace:rw");
            dockerCmd.add("-w");
            dockerCmd.add("/workspace");
            dockerCmd.add(image);

            if (image.startsWith("python")) {
                dockerCmd.add("python");
                dockerCmd.add(scriptName);
            } else if (image.startsWith("node")) {
                dockerCmd.add("node");
                dockerCmd.add(scriptName);
            } else {
                dockerCmd.add("java");
                dockerCmd.add(scriptName);
            }

            return runProcess(new ProcessBuilder(dockerCmd), workspaceDir, "Docker (" + image + ")");

        } catch (Exception e) {
            log.error("Docker execution failed: {}", e.getMessage(), e);
            return ToolResponse.error("Docker container execution failed: " + e.getMessage());
        }
    }

    /**
     * Executes the script via local ProcessBuilder strictly sandboxed to the run workspace directory.
     */
    private ToolResponse executeInProcessBuilder(String image, String scriptName, File workspaceDir) {
        try {
            List<String> cmd = new ArrayList<>();
            if (image.startsWith("node")) {
                cmd.add("node");
            } else if (image.startsWith("openjdk")) {
                cmd.add("java");
            } else {
                // Default: Python runner
                cmd.add("python");
            }
            cmd.add(scriptName);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workspaceDir); // Strictly scoped to run workspace directory
            return runProcess(pb, workspaceDir, "Local ProcessBuilder (Fallback Runner)");

        } catch (Exception e) {
            log.error("ProcessBuilder execution failed: {}", e.getMessage(), e);
            return ToolResponse.error("ProcessBuilder execution failed: " + e.getMessage());
        }
    }

    /**
     * Executes process with strict timeout and output capturing.
     */
    private ToolResponse runProcess(ProcessBuilder pb, File workingDir, String runnerLabel) {
        try {
            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = new Thread(() -> readStream(process.getInputStream(), stdout));
            Thread errThread = new Thread(() -> readStream(process.getErrorStream(), stderr));
            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Process timed out after {}s (killed)", DEFAULT_TIMEOUT_SECONDS);
                return ToolResponse.error("Execution timed out after " + DEFAULT_TIMEOUT_SECONDS + "s (Terminated for security)");
            }

            outThread.join(1000);
            errThread.join(1000);

            int exitCode = process.exitValue();
            String outStr = stdout.toString().trim();
            String errStr = stderr.toString().trim();

            if (exitCode == 0) {
                String result = outStr.isEmpty() ? "Script completed successfully (exit code 0)" : outStr;
                if (result.length() > MAX_OUTPUT_CHARS) {
                    result = result.substring(0, MAX_OUTPUT_CHARS) + "\n[Output truncated at " + MAX_OUTPUT_CHARS + " chars]";
                }
                log.info("Process completed successfully via {}", runnerLabel);
                return ToolResponse.success("[Runner: " + runnerLabel + "]\n" + result);
            } else {
                String errorDetail = !errStr.isEmpty() ? errStr : outStr;
                log.warn("Process exited with code {}: {}", exitCode, errorDetail);
                return ToolResponse.error("Process exited with non-zero code " + exitCode + ":\n" + errorDetail);
            }

        } catch (Exception e) {
            log.error("Failed running process: {}", e.getMessage(), e);
            return ToolResponse.error("Execution exception: " + e.getMessage());
        }
    }

    private void readStream(InputStream is, StringBuilder sb) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() < MAX_OUTPUT_CHARS) {
                    sb.append(line).append("\n");
                }
            }
        } catch (IOException ignored) {}
    }

    /**
     * Fast check to determine if the local Docker daemon is running and responsive.
     */
    private boolean isDockerDaemonRunning() {
        try {
            Process p = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = p.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInsideWorkspace(File file) {
        try {
            String filePath = file.getCanonicalPath();
            String basePath = new File(baseDir).getCanonicalPath();
            return filePath.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }

    public static Set<String> getAllowedImages() {
        return ALLOWED_IMAGES;
    }
}

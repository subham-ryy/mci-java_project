package com.mci.agents;

import com.mci.core.*;
import com.mci.gateway.ToolGateway;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Dedicated agent for sandboxed code execution, computation, and autonomous self-correction.
 *
 * Implements the autonomous Write -> Execute -> Error Analysis -> Fix-and-Retry Loop:
 * - Writes solution script to the run's workspace via ToolGateway ("file.write")
 * - Executes script in isolated container/sandbox via ToolGateway ("docker.run")
 * - If execution encounters a syntax error or non-zero exit code, feeds the error traceback
 *   back to Gemini to produce a corrected script, repeating up to 3 attempts.
 */
public class DockerAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DockerAgent.class);
    private static final int MAX_FIX_ATTEMPTS = 3;

    private final ChatLanguageModel model;
    private final ToolGateway gateway;

    public DockerAgent(ChatLanguageModel model, ToolGateway gateway) {
        this.model = model;
        this.gateway = gateway;
    }

    @Override
    public AgentResult execute(SubTask subTask) {
        log.info("DockerAgent executing computational subtask: {}", subTask.description());

        String currentCode = null;
        String lastError = null;
        String explanation = "Initial computational execution";

        for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
            try {
                // Step 1: Generate or fix code using Gemini
                if (attempt == 1) {
                    String prompt = """
                            You are a senior DOCKER and software computation agent in a multi-agent intelligence system.
                            Your subtask is: "%s"

                            Write a complete, self-contained, robust Python 3 script to perform the requested calculation, simulation, or data processing.
                            Requirements:
                            - The script must print clear, structured results and key performance metrics to stdout.
                            - Do not rely on external non-standard libraries (use Python standard libraries such as math, statistics, time, sys, json, os).
                            - Include basic error handling.

                            Format your response EXACTLY as:
                            ===EXPLANATION===
                            <1-2 sentences explaining what the script computes>
                            ===CODE===
                            <raw python code only, without markdown backticks>
                            """.formatted(subTask.description());

                    String response = LlmFactory.generateWithRetry(model, prompt);
                    ParsedScript parsed = parseScript(response);
                    currentCode = parsed.code;
                    explanation = parsed.explanation;

                } else {
                    log.info("DockerAgent attempt {}/{}: autonomous fix-and-retry loop...", attempt, MAX_FIX_ATTEMPTS);
                    String fixPrompt = """
                            You are a DOCKER software computation agent.
                            The previous script execution failed with the following error/traceback:

                            ERROR TRACEBACK:
                            %s

                            PREVIOUS FAILING CODE:
                            %s

                            Analyze the error and provide a corrected, working Python 3 script.
                            Format your response EXACTLY as:
                            ===EXPLANATION===
                            <explanation of the bug and fix applied>
                            ===CODE===
                            <corrected python code only, without markdown backticks>
                            """.formatted(lastError, currentCode);

                    String response = LlmFactory.generateWithRetry(model, fixPrompt);
                    ParsedScript parsed = parseScript(response);
                    currentCode = parsed.code;
                    explanation = "Fix applied (Attempt " + attempt + "): " + parsed.explanation;
                }

                // Step 2: Write script to workspace via ToolGateway
                String scriptPath = subTask.runId() + "/solution.py";
                ToolRequest writeRequest = new ToolRequest(
                        subTask.runId(),
                        subTask.id(),
                        AgentType.DOCKER,
                        "file.write",
                        Map.of("path", scriptPath, "content", currentCode)
                );
                ToolResponse writeResponse = gateway.execute(writeRequest);
                if (!writeResponse.success()) {
                    return AgentResult.failure("Failed saving script to workspace: " + writeResponse.error());
                }

                // Step 3: Execute script via ToolGateway (docker.run)
                ToolRequest execRequest = new ToolRequest(
                        subTask.runId(),
                        subTask.id(),
                        AgentType.DOCKER,
                        "docker.run",
                        Map.of(
                                "image", "python:3.12-slim",
                                "scriptName", "solution.py",
                                "command", "python solution.py"
                        )
                );
                ToolResponse execResponse = gateway.execute(execRequest);

                // Step 4: Evaluate execution outcome
                if (execResponse.success()) {
                    String output = execResponse.output();
                    log.info("DockerAgent execution succeeded on attempt {}", attempt);

                    // Save output artifact to workspace/docker_output.txt via ToolGateway
                    String outputPath = subTask.runId() + "/docker_output.txt";
                    ToolRequest writeLogRequest = new ToolRequest(
                            subTask.runId(),
                            subTask.id(),
                            AgentType.DOCKER,
                            "file.write",
                            Map.of("path", outputPath, "content", output)
                    );
                    gateway.execute(writeLogRequest);

                    // Extract preview summary
                    String preview = explanation;
                    if (output.contains("\n")) {
                        String[] lines = output.split("\n");
                        for (String line : lines) {
                            if (!line.trim().startsWith("[Runner:") && !line.trim().isBlank()) {
                                preview = line.trim();
                                break;
                            }
                        }
                    }

                    return AgentResult.success(preview
                            + "\n[Script Saved: " + scriptPath + "]"
                            + "\n[Docker Output: " + outputPath + "]");

                } else {
                    lastError = execResponse.error();
                    log.warn("DockerAgent attempt {} failed: {}", attempt, lastError);
                    if (attempt == MAX_FIX_ATTEMPTS) {
                        return AgentResult.failure("Docker execution failed after " + MAX_FIX_ATTEMPTS + " attempts: " + lastError);
                    }
                }

            } catch (Exception e) {
                lastError = e.getMessage();
                log.error("DockerAgent exception on attempt {}: {}", attempt, e.getMessage(), e);
                if (attempt == MAX_FIX_ATTEMPTS) {
                    return AgentResult.failure("DockerAgent error: " + e.getMessage());
                }
            }
        }

        return AgentResult.failure("DockerAgent failed: " + lastError);
    }

    private static ParsedScript parseScript(String response) {
        String explanation = "Executed computation script";
        String code = response;

        if (response.contains("===EXPLANATION===") && response.contains("===CODE===")) {
            int expIdx = response.indexOf("===EXPLANATION===") + 17;
            int codeIdx = response.indexOf("===CODE===");
            explanation = response.substring(expIdx, codeIdx).trim();
            code = response.substring(codeIdx + 10).trim();
        } else if (response.contains("===CODE===")) {
            code = response.substring(response.indexOf("===CODE===") + 10).trim();
        }

        // Clean any code block markers if LLM wrapped in backticks
        if (code.startsWith("```python")) code = code.substring(9);
        if (code.startsWith("```")) code = code.substring(3);
        if (code.endsWith("```")) code = code.substring(0, code.length() - 3);

        return new ParsedScript(explanation.trim(), code.trim());
    }

    private record ParsedScript(String explanation, String code) {}
}

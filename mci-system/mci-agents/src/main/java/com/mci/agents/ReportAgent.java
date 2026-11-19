package com.mci.agents;

import com.mci.core.*;
import com.mci.gateway.ToolGateway;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Dedicated agent for synthesizing multi-agent findings and producing the executive
 * Mission Debrief and Summary Report (report.md).
 *
 * Inspects all real execution artifacts produced by preceding agents in the run workspace:
 * - sources.txt (external web research gathered by NavigatorAgent)
 * - solution.py (computational script authored by DockerAgent)
 * - docker_output.txt (sandboxed execution output, logs, and benchmark metrics)
 * - data.json (structured metrics gathered by DataFileAgent)
 * - chart.png (visual plots and graphs created by DockerAgent)
 *
 * Saves report.md in the workspace run folder strictly through the ToolGateway.
 */
public class ReportAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ReportAgent.class);

    private final ChatLanguageModel model;
    private final ToolGateway gateway;

    public ReportAgent(ChatLanguageModel model, ToolGateway gateway) {
        this.model = model;
        this.gateway = gateway;
    }

    @Override
    public AgentResult execute(SubTask subTask) {
        log.info("ReportAgent compiling executive mission report for: {}", subTask.description());

        try {
            String runId = subTask.runId();

            // Step 1: Read all actual artifacts produced by preceding agents
            String dockerOutput = readFileContent(runId, subTask.id(), "docker_output.txt");
            String pythonScript = readFileContent(runId, subTask.id(), "solution.py");
            String sourcesText = readFileContent(runId, subTask.id(), "sources.txt");
            String dataJson = readFileContent(runId, subTask.id(), "data.json");
            boolean hasChart = checkFileExists(runId, "chart.png");
            boolean hasPdf = checkFileExists(runId, "results.pdf");

            // Step 2: Build factual, context-rich prompt for Gemini
            StringBuilder prompt = new StringBuilder();
            prompt.append("""
                    You are the senior REPORT SYNTHESIZER agent in an autonomous multi-agent intelligence system.
                    Your objective is to produce a genuine, factual, and data-driven Executive Mission Debrief (report.md).

                    CRITICAL INSTRUCTIONS:
                    1. Do NOT write a generic theoretical essay or invent hypothetical numbers.
                    2. Base your entire report directly on the REAL execution artifacts gathered below.
                    3. Cite the actual numbers, calculations, algorithm logic, and console output from the run.
                    4. Quote the real web sources and URLs if research was performed.
                    5. Summarize what each agent did and the final outcome of the mission.

                    """);

            prompt.append("MISSION OBJECTIVE / TASK:\n").append(subTask.description()).append("\n\n");
            prompt.append("=== REAL EXECUTION ARTIFACTS GATHERED FROM THIS RUN ===\n\n");

            if (dockerOutput != null && !dockerOutput.isBlank()) {
                prompt.append("--- [DOCKER SANDBOX OUTPUT (docker_output.txt)] ---\n")
                      .append(dockerOutput).append("\n\n");
            } else {
                prompt.append("--- [DOCKER SANDBOX OUTPUT] ---\nNo container code execution output in this run.\n\n");
            }

            if (pythonScript != null && !pythonScript.isBlank()) {
                prompt.append("--- [COMPUTATIONAL SCRIPT (solution.py)] ---\n")
                      .append(pythonScript).append("\n\n");
            }

            if (sourcesText != null && !sourcesText.isBlank()) {
                prompt.append("--- [NAVIGATOR WEB SOURCES (sources.txt)] ---\n")
                      .append(sourcesText).append("\n\n");
            } else {
                prompt.append("--- [NAVIGATOR WEB SOURCES] ---\nNo external web sources fetched in this run.\n\n");
            }

            if (dataJson != null && !dataJson.isBlank()) {
                prompt.append("--- [DATASET METRICS (data.json)] ---\n")
                      .append(dataJson).append("\n\n");
            }

            if (hasChart) {
                prompt.append("--- [VISUAL DELIVERABLE] ---\n")
                      .append("A visual chart was generated and saved as chart.png. Reference and embed it in the report using: ![Visual Analysis](chart.png)\n\n");
            }

            if (hasPdf) {
                prompt.append("--- [PDF DELIVERABLE] ---\n")
                      .append("A structured PDF result document was generated and saved as results.pdf.\n\n");
            }

            prompt.append("""
                    Produce the report in clean Markdown using this exact executive structure:

                    # Executive Mission Debrief: <Specific Mission Title>

                    ## 1. Mission Overview & Execution Status
                    - State the original mission objective clearly.
                    - Executive summary of the multi-agent execution workflow.
                    - State overall status (e.g. ALL SUBTASKS COMPLETED SUCCESSFULLY).

                    ## 2. Research & Intelligence Findings
                    - Synthesize findings from external sources (quote the real URLs and facts from sources.txt if available).
                    - If no external browsing was needed, describe the analytical scope.

                    ## 3. Computational Results & Benchmarks
                    - Summarize the algorithm executed in solution.py.
                    - Quote the real output, calculations, and performance benchmarks printed in docker_output.txt.
                    - Highlight key numerical values and mathematical/computational findings.

                    ## 4. Visualizations & Generated Deliverables
                    - List all files produced during this mission with descriptions (solution.py, docker_output.txt, chart.png, results.pdf, sources.txt).
                    - If chart.png was generated, include '![Visual Analysis](chart.png)' and describe what the graph depicts.

                    ## 5. Multi-Agent Security & Governance Audit
                    - Confirm that all tool executions (file I/O, web fetch, container execution) passed through the Zero-Trust ToolGateway.
                    - Confirm complete sandbox containment and audit trail logging.

                    ## 6. Strategic Takeaways & Recommendations
                    - 2-3 actionable conclusions drawn directly from the computed data and research findings.
                    """);

            String reportContent = LlmFactory.generateWithRetry(model, prompt.toString());

            // Step 3: Write Markdown report via ToolGateway (report.md)
            String reportPath = runId + "/report.md";
            ToolRequest writeRequest = new ToolRequest(
                    runId,
                    subTask.id(),
                    AgentType.REPORT,
                    "file.write",
                    Map.of("path", reportPath, "content", reportContent)
            );
            ToolResponse writeResponse = gateway.execute(writeRequest);

            if (writeResponse.success()) {
                return AgentResult.success("Executive Markdown report compiled successfully.\n[Report Saved: " + reportPath + "]");
            } else {
                return AgentResult.failure("Gateway rejected report write: " + writeResponse.error());
            }

        } catch (Exception e) {
            log.error("ReportAgent failed: {}", e.getMessage(), e);
            return AgentResult.failure("ReportAgent error: " + e.getMessage());
        }
    }

    private String readFileContent(String runId, String subTaskId, String filename) {
        try {
            ToolRequest readRequest = new ToolRequest(
                    runId,
                    subTaskId,
                    AgentType.REPORT,
                    "file.read",
                    Map.of("path", runId + "/" + filename)
            );
            ToolResponse response = gateway.execute(readRequest);
            if (response.success() && response.output() != null && !response.output().isBlank()) {
                return response.output().trim();
            }
        } catch (Exception e) {
            log.warn("Failed reading {} for report synthesis: {}", filename, e.getMessage());
        }
        return null;
    }

    private boolean checkFileExists(String runId, String filename) {
        try {
            File f1 = new File("./workspace/" + runId, filename);
            if (f1.exists()) return true;
            File f2 = new File("../workspace/" + runId, filename);
            if (f2.exists()) return true;
            File f3 = new File("workspace/" + runId, filename);
            return f3.exists();
        } catch (Exception e) {
            return false;
        }
    }
}

package com.mci.agents;

import com.mci.core.*;
import com.mci.gateway.ToolGateway;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Dedicated agent for data processing, in-depth research, and results generation.
 *
 * Saves raw metrics to data.json and compiles comprehensive research results
 * to results.pdf strictly through the ToolGateway.
 */
public class DataFileAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DataFileAgent.class);

    private final ChatLanguageModel model;
    private final ToolGateway gateway;

    public DataFileAgent(ChatLanguageModel model, ToolGateway gateway) {
        this.model = model;
        this.gateway = gateway;
    }

    @Override
    public AgentResult execute(SubTask subTask) {
        log.info("DataFileAgent executing research and results generation for: {}", subTask.description());

        try {
            String prompt = """
                    You are a senior RESEARCH and DATA ANALYSIS agent in a multi-agent intelligence system.
                    Your subtask is: %s

                    Perform thorough, professional research and data extraction.
                    You must produce structured outputs with clear headings, detailed analysis, and concrete quantitative metrics.
                    Leave clear blank lines between headings and paragraphs so that content is clean and well-spaced.

                    Format your response EXACTLY in three sections:

                    ===SUMMARY===
                    <2-3 sentence executive overview of what was researched and analyzed>

                    ===DATA_JSON===
                    {
                      "topic": "Research & Analysis",
                      "metrics": {
                        "metric_1": "value_1",
                        "metric_2": "value_2",
                        "metric_3": "value_3"
                      },
                      "key_findings": [
                        "finding 1",
                        "finding 2",
                        "finding 3"
                      ]
                    }

                    ===RESULTS_DOCUMENT===
                    # Research & Intelligence Results: Analysis

                    ## 1. Executive Overview & Context
                    <Detailed paragraph explaining the context, background, and scope of this research.>

                    ## 2. Key Research Findings & Intelligence
                    <Detailed analysis of primary trends and core observations.>

                    - Finding 1: <Detailed description with industry context>
                    - Finding 2: <Detailed description with industry context>
                    - Finding 3: <Detailed description with industry context>

                    ## 3. Quantitative Benchmarks & Key Metrics
                    - Primary Growth Rate: <Value> - <Explanation>
                    - Adoption / Efficiency Metric: <Value> - <Explanation>
                    - Implementation Velocity: <Value> - <Explanation>
                    - Risk & Governance Score: <Value> - <Explanation>

                    ## 4. Strategic Assessment & Actionable Next Steps
                    <Detailed paragraph synthesizing the implications of the results.>

                    - Short-Term Initiative: <Actionable item>
                    - Strategic Priority: <Actionable item>
                    """
                    .formatted(subTask.description());

            String response = LlmFactory.generateWithRetry(model, prompt);

            String summary = "Research and data analysis completed.";
            String dataJson = null;
            String resultsDoc = null;

            // Robust parsing of sections
            if (response.contains("===SUMMARY===") && response.contains("===RESULTS_DOCUMENT===")) {
                int sumIdx = response.indexOf("===SUMMARY===") + 13;
                int jsonIdx = response.indexOf("===DATA_JSON===");
                int docIdx = response.indexOf("===RESULTS_DOCUMENT===");

                if (jsonIdx != -1 && jsonIdx > sumIdx) {
                    summary = response.substring(sumIdx, jsonIdx).trim();
                    dataJson = response.substring(jsonIdx + 15, docIdx).trim();
                } else {
                    summary = response.substring(sumIdx, docIdx).trim();
                }
                resultsDoc = response.substring(docIdx + 22).trim();
            } else if (response.contains("===RESULTS_DOCUMENT===")) {
                resultsDoc = response.substring(response.indexOf("===RESULTS_DOCUMENT===") + 22).trim();
                summary = "Research results generated successfully.";
            } else {
                // Fallback: treat response as results document
                resultsDoc = response.trim();
                summary = "Completed research analysis.";
            }

            // Clean up any markdown json blocks if present in dataJson
            if (dataJson != null) {
                if (dataJson.startsWith("```json")) dataJson = dataJson.substring(7);
                if (dataJson.startsWith("```")) dataJson = dataJson.substring(3);
                if (dataJson.endsWith("```")) dataJson = dataJson.substring(0, dataJson.length() - 3);
                dataJson = dataJson.trim();
            }

            // Step 1: Save data.json via ToolGateway
            if (dataJson != null && !dataJson.isBlank()) {
                String jsonPath = subTask.runId() + "/data.json";
                ToolRequest writeJsonReq = new ToolRequest(
                        subTask.runId(),
                        subTask.id(),
                        AgentType.DATA_FILE,
                        "file.write",
                        Map.of("path", jsonPath, "content", dataJson)
                );
                gateway.execute(writeJsonReq);
            }

            // Step 2: Generate results.pdf via ToolGateway
            String pdfPath = subTask.runId() + "/results.pdf";
            String title = "Research Results — " + subTask.runId();
            if (resultsDoc != null && resultsDoc.contains("# ")) {
                int start = resultsDoc.indexOf("# ") + 2;
                int end = resultsDoc.indexOf("\n", start);
                if (end != -1) {
                    title = resultsDoc.substring(start, end).trim();
                }
            }

            ToolRequest pdfReq = new ToolRequest(
                    subTask.runId(),
                    subTask.id(),
                    AgentType.DATA_FILE,
                    "pdf.generate",
                    Map.of(
                            "path", pdfPath,
                            "content", resultsDoc != null ? resultsDoc : response,
                            "title", title
                    )
            );
            ToolResponse pdfResp = gateway.execute(pdfReq);

            if (pdfResp.success()) {
                return AgentResult.success(summary
                        + "\n[Results PDF Saved: " + pdfPath + "]"
                        + (dataJson != null ? "\n[Data File Saved: " + subTask.runId() + "/data.json]" : ""));
            } else {
                return AgentResult.failure("PDF generation failed: " + pdfResp.error());
            }

        } catch (Exception e) {
            log.error("DataFileAgent failed: {}", e.getMessage(), e);
            return AgentResult.failure("DataFileAgent error: " + e.getMessage());
        }
    }
}

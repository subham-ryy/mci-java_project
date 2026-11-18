package com.mci.agents;

import com.mci.core.*;
import com.mci.gateway.ToolGateway;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A general-purpose agent that uses Gemini to process subtasks,
 * executing operations strictly through the ToolGateway.
 */
public class SimpleAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SimpleAgent.class);

    private final ChatLanguageModel model;
    private final ToolGateway gateway;

    public SimpleAgent(ChatLanguageModel model, ToolGateway gateway) {
        this.model = model;
        this.gateway = gateway;
    }

    @Override
    public AgentResult execute(SubTask subTask) {
        log.info("Agent [{}] executing: {}", subTask.agentType(), subTask.description());

        try {
            // Step 1: Ask Gemini to process the task
            String prompt = "You are a " + subTask.agentType() + " agent in a multi-agent system. "
                    + "Your task is: " + subTask.description() + "\n"
                    + "Provide a clear, concise result for this task. "
                    + "Keep your response short (2-3 sentences max).";

            String llmResponse = LlmFactory.generateWithRetry(model, prompt);
            log.info("Gemini response: {}", llmResponse);

            // Step 2: If this is a REPORT agent, write the result to a file
            if (subTask.agentType() == AgentType.REPORT) {
                String reportPath = subTask.runId() + "/report.md";
                ToolRequest writeRequest = new ToolRequest(
                        subTask.runId(),
                        subTask.id(),
                        subTask.agentType(),
                        "file.write",
                        Map.of("path", reportPath, "content", llmResponse)
                );
                ToolResponse writeResponse = gateway.execute(writeRequest);
                if (writeResponse.success()) {
                    return AgentResult.success("Report written to " + reportPath + "\n" + llmResponse);
                }
            }

            // Step 3: If this is a DATA_FILE agent doing a read, use the gateway
            if (subTask.agentType() == AgentType.DATA_FILE
                    && subTask.description().toLowerCase().contains("read")) {
                String filePath = subTask.runId() + "/data.txt";
                ToolRequest readRequest = new ToolRequest(
                        subTask.runId(),
                        subTask.id(),
                        subTask.agentType(),
                        "file.read",
                        Map.of("path", filePath)
                );
                ToolResponse readResponse = gateway.execute(readRequest);
                if (readResponse.success()) {
                    return AgentResult.success("File contents: " + readResponse.output()
                            + "\nAI Analysis: " + llmResponse);
                }
            }

            // Default: just return the LLM response as the result
            return AgentResult.success(llmResponse);

        } catch (Exception e) {
            log.error("Agent execution failed: {}", e.getMessage());
            return AgentResult.failure("Agent error: " + e.getMessage());
        }
    }
}

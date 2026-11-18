package com.mci.agents;

import com.mci.core.*;
import com.mci.gateway.ToolGateway;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated agent for web navigation, online intelligence gathering, and source extraction.
 *
 * Interacts with external web pages strictly via the ToolGateway ("browser.fetch").
 */
public class NavigatorAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(NavigatorAgent.class);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[a-zA-Z0-9.-]+(?:/[^\\s\"'<>()]*)?");

    private final ChatLanguageModel model;
    private final ToolGateway gateway;

    public NavigatorAgent(ChatLanguageModel model, ToolGateway gateway) {
        this.model = model;
        this.gateway = gateway;
    }

    @Override
    public AgentResult execute(SubTask subTask) {
        log.info("NavigatorAgent executing research task: {}", subTask.description());

        try {
            // Step 1: Detect explicit URL or identify authoritative reference URL
            String targetUrl = extractUrl(subTask.description());

            if (targetUrl == null) {
                String prompt = """
                        You are a web research and navigation specialist.
                        The subtask is: "%s"

                        Select ONE public, authoritative HTTPS URL (such as an en.wikipedia.org article or standard documentation) most suitable to research this topic.
                        Output ONLY the raw URL (e.g., https://en.wikipedia.org/wiki/Artificial_intelligence), with no explanation or extra text.
                        """.formatted(subTask.description());

                String suggestion = LlmFactory.generateWithRetry(model, prompt).trim();
                targetUrl = extractUrl(suggestion);

                if (targetUrl == null) {
                    targetUrl = "https://en.wikipedia.org/wiki/Artificial_intelligence";
                }
            }

            log.info("NavigatorAgent fetching URL via gateway: {}", targetUrl);

            // Step 2: Fetch the web page strictly through the ToolGateway
            ToolRequest fetchRequest = new ToolRequest(
                    subTask.runId(),
                    subTask.id(),
                    AgentType.NAVIGATOR,
                    "browser.fetch",
                    Map.of("url", targetUrl)
            );
            ToolResponse fetchResponse = gateway.execute(fetchRequest);

            if (fetchResponse.denied()) {
                return AgentResult.failure("Gateway blocked web navigation: " + fetchResponse.error());
            }

            String webContent = fetchResponse.output();
            if (!fetchResponse.success() || webContent == null || webContent.isBlank()) {
                webContent = "Source URL: " + targetUrl + " (Network response unavailable: " + fetchResponse.error() + ")";
            }

            // Step 3: Analyze and synthesize extracted web content
            String analysisPrompt = """
                    You are a NAVIGATOR agent in a multi-agent system.
                    Your subtask was: %s
                    Target URL: %s

                    Extracted Web Content:
                    %s

                    Analyze this live web content and extract structured findings:
                    1. Provide a concise summary of what was found.
                    2. List 3-4 specific facts, data points, or metrics extracted from the web source.

                    Format your response as:
                    SUMMARY: <2-3 sentence overview of the web findings>
                    KEY_FINDINGS:
                    - <Finding 1 with data/metric>
                    - <Finding 2 with data/metric>
                    - <Finding 3 with data/metric>
                    """.formatted(subTask.description(), targetUrl, webContent);

            String analysisResponse = LlmFactory.generateWithRetry(model, analysisPrompt);

            // Step 4: Save raw sources to workspace/<runId>/sources.txt via ToolGateway
            String sourcesPath = subTask.runId() + "/sources.txt";
            String filePayload = "=== MCI NAVIGATOR SOURCE REPORT ===\n"
                    + "URL: " + targetUrl + "\n"
                    + "Subtask: " + subTask.description() + "\n\n"
                    + "=== SYNTHESIS ===\n" + analysisResponse + "\n\n"
                    + "=== EXTRACTED CONTENT ===\n" + webContent;

            ToolRequest writeRequest = new ToolRequest(
                    subTask.runId(),
                    subTask.id(),
                    AgentType.NAVIGATOR,
                    "file.write",
                    Map.of("path", sourcesPath, "content", filePayload)
            );
            gateway.execute(writeRequest);

            // Extract the clean summary for the scheduler preview
            String preview = analysisResponse;
            if (analysisResponse.contains("SUMMARY:")) {
                int start = analysisResponse.indexOf("SUMMARY:") + 8;
                int end = analysisResponse.indexOf("\n", start);
                if (end != -1) {
                    preview = analysisResponse.substring(start, end).trim();
                }
            }

            return AgentResult.success(preview
                    + "\n[URL Fetched: " + targetUrl + "]"
                    + "\n[Sources Saved: " + sourcesPath + "]");

        } catch (Exception e) {
            log.error("NavigatorAgent failed: {}", e.getMessage(), e);
            return AgentResult.failure("NavigatorAgent error: " + e.getMessage());
        }
    }

    private String extractUrl(String text) {
        if (text == null) return null;
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(0).trim();
            while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith(";")) {
                url = url.substring(0, url.length() - 1);
            }
            return url;
        }
        return null;
    }
}

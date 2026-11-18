package com.mci.agents;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Creates the LLM (Large Language Model) connection to Google Gemini.
 *
 * This connects directly to Google's live Generative Language API via LangChain4j.
 * Supports the Gemini 3.x generation (gemini-3.8-flash, gemini-3.5-flash-lite).
 */
public class LlmFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmFactory.class);

    public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";

    /**
     * Creates a live Gemini chat model using the provided API key and default model.
     */
    public static ChatLanguageModel createGemini(String apiKey) {
        return createGemini(apiKey, DEFAULT_MODEL);
    }

    /**
     * Creates a live Gemini chat model with a specific model name.
     */
    public static ChatLanguageModel createGemini(String apiKey, String modelName) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        if (modelName == null || modelName.isBlank()) {
            modelName = DEFAULT_MODEL;
        }

        log.info("Connecting to live Google Gemini AI (model: {})...", modelName);

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(30))
                .build();

        log.info("Gemini AI connected successfully (model: {})", modelName);
        return model;
    }

    /**
     * Resilient execution that retries on temporary Google 503 high-demand spikes or rate limits.
     */
    public static String generateWithRetry(ChatLanguageModel model, String prompt) {
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return model.generate(prompt);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean isTransient = msg.contains("503") || msg.contains("429")
                        || msg.contains("UNAVAILABLE") || msg.contains("high demand")
                        || msg.contains("temporarily");

                if (isTransient && attempt < maxAttempts) {
                    log.warn("Gemini service busy (503/429), backing off for attempt {}/{}...", attempt + 1, maxAttempts);
                    try {
                        Thread.sleep(2000L * attempt);
                    } catch (InterruptedException ignored) {}
                } else {
                    throw e;
                }
            }
        }
        return model.generate(prompt);
    }

    /**
     * Quick test — sends "Hello" to Gemini and prints the response.
     */
    public static String testConnection(String apiKey) {
        ChatLanguageModel model = createGemini(apiKey);
        String response = generateWithRetry(model, "Say 'Hello from Gemini!' in one short sentence.");
        log.info("Gemini test response: {}", response);
        return response;
    }
}

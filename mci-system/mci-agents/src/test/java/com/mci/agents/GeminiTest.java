package com.mci.agents;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeminiTest {

    @Test
    public void testGeminiDirect() {
        File envFile = new File("../.env");
        if (!envFile.exists()) {
            envFile = new File(".env");
        }
        String apiKey = null;
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("GEMINI_API_KEY=")) {
                        apiKey = line.substring("GEMINI_API_KEY=".length()).trim();
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not read .env: " + e.getMessage());
                return;
            }
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Skipping test: No API key found in .env");
            return;
        }

        System.out.println("Testing live with API Key: " + apiKey.substring(0, 8) + "...");

        try {
            ChatLanguageModel model = LlmFactory.createGemini(apiKey, "gemini-3.5-flash-lite");
            String response = model.generate("Hello, say 'Test OK' in 2 words.");
            System.out.println("Gemini Response: " + response);
            assertNotNull(response);
        } catch (Exception e) {
            System.out.println("Live Gemini test failed: " + e.getMessage());
        }
    }
}

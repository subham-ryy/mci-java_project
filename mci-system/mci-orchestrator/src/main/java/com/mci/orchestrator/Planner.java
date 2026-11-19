package com.mci.orchestrator;

import com.mci.core.AgentType;
import com.mci.core.SubTask;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Planner asks Gemini to break a user's objective into subtasks.
 *
 * Breaks down objectives into specialized agent tasks:
 * - DATA_FILE agent conducts research, analyzes data, and produces results.pdf & data.json
 * - REPORT agent synthesizes findings into report.md
 */
public class Planner {

    private static final Logger log = LoggerFactory.getLogger(Planner.class);

    private final ChatLanguageModel model;

    public Planner(ChatLanguageModel model) {
        this.model = model;
    }

    /**
     * Creates a plan (list of subtasks) for the given objective.
     *
     * @param runId     the run ID these subtasks belong to
     * @param objective the user's goal in plain English
     * @return a list of SubTasks to execute
     */
    public List<SubTask> plan(String runId, String objective) {
        log.info("Planning subtasks for: {}", objective);

        try {
            // Ask Gemini to create a plan
            String prompt = """
                    You are a task planner for a multi-agent system. Break the following objective into 2-3 logical steps.
                    For each step, specify:
                    - A short description (one line)
                    - The agent type: DATA_FILE, NAVIGATOR, DOCKER, or REPORT

                    Rules:
                    - NAVIGATOR agent browses external websites and fetches live web pages (e.g. articles, technical overviews, URLs) to extract real-world facts and saves sources.txt
                    - DATA_FILE agent conducts in-depth data analysis, calculates metrics, and compiles results into results.pdf and data.json
                    - DOCKER agent writes and executes code or scripts in sandboxed containers (docker.run) to perform simulations, computational processing, benchmarks, algorithms, and data visualizations/charts (matplotlib, numpy, chart.png) when requested
                    - REPORT agent always comes LAST and synthesizes all findings into the final executive Markdown report (report.md)
                    - The last step should ALWAYS be a REPORT step

                    Format each step EXACTLY like this (one per line):
                    STEP|AGENT_TYPE|description

                    Example:
                    STEP|DATA_FILE|Research enterprise AI adoption trends, benchmark metrics, and compile results into results.pdf
                    STEP|REPORT|Synthesize research findings into final executive report.md

                    Objective: """ + objective;

            String response = com.mci.agents.LlmFactory.generateWithRetry(model, prompt);
            log.info("Planner LLM response:\n{}", response);

            // Parse the response into SubTasks
            List<SubTask> subtasks = parseResponse(runId, response);

            if (subtasks.isEmpty()) {
                log.warn("LLM returned no parseable steps, using default plan");
                return defaultPlan(runId, objective);
            }

            return subtasks;

        } catch (Exception e) {
            log.error("Planning failed: {}, using default plan", e.getMessage());
            return defaultPlan(runId, objective);
        }
    }

    /**
     * Parses the LLM response into SubTask objects.
     * Expects lines in format: STEP|AGENT_TYPE|description
     */
    private List<SubTask> parseResponse(String runId, String response) {
        List<SubTask> subtasks = new ArrayList<>();
        String[] lines = response.split("\n");
        int seq = 1;

        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("STEP|")) continue;

            String[] parts = line.split("\\|", 3);
            if (parts.length < 3) continue;

            try {
                AgentType agentType = AgentType.valueOf(parts[1].trim());
                String description = parts[2].trim();
                String id = runId + "-st-" + seq;

                // Each step depends on the previous one (sequential plan)
                List<String> deps = seq > 1 ? List.of(runId + "-st-" + (seq - 1)) : List.of();

                subtasks.add(SubTask.create(id, runId, seq, agentType, description, deps));
                seq++;

            } catch (IllegalArgumentException e) {
                log.warn("Skipping unrecognized agent type in: {}", line);
            }
        }

        return subtasks;
    }

    /**
     * Fallback plan if the LLM fails — always works, no AI needed.
     */
    private List<SubTask> defaultPlan(String runId, String objective) {
        log.info("Using default 2-step plan");
        return List.of(
                SubTask.create(runId + "-st-1", runId, 1, AgentType.DATA_FILE,
                        "Research objective, extract metrics, and compile results.pdf: " + objective, List.of()),
                SubTask.create(runId + "-st-2", runId, 2, AgentType.REPORT,
                        "Synthesize findings into executive report.md", List.of(runId + "-st-1"))
        );
    }
}

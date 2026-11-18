package com.mci.web.config;

import com.mci.agents.AgentDispatcher;
import com.mci.agents.LlmFactory;
import com.mci.gateway.PermissionPolicy;
import com.mci.gateway.ToolGateway;
import com.mci.orchestrator.Planner;
import com.mci.orchestrator.Scheduler;
import com.mci.persistence.ArtifactDao;
import com.mci.persistence.AuditDao;
import com.mci.persistence.Database;
import com.mci.persistence.RunDao;
import com.mci.persistence.SubTaskDao;
import com.mci.tools.BrowserTool;
import com.mci.tools.DockerTool;
import com.mci.tools.FileTool;
import com.mci.tools.FileWriterTool;
import com.mci.tools.PdfTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MciConfig {

    private static final Logger log = LoggerFactory.getLogger(MciConfig.class);

    @Bean
    public Database database() {
        File dataDir = resolveBaseDir("data");
        File dbFile = new File(dataDir, "mci.db");
        Database db = new Database(dbFile.getPath());
        db.initSchema();
        log.info("[✓] SQLite database initialized at {}", dbFile.getPath());
        return db;
    }

    @Bean
    public RunDao runDao(Database database) {
        return new RunDao(database);
    }

    @Bean
    public SubTaskDao subTaskDao(Database database) {
        return new SubTaskDao(database);
    }

    @Bean
    public AuditDao auditDao(Database database) {
        return new AuditDao(database);
    }

    @Bean
    public ArtifactDao artifactDao(Database database) {
        return new ArtifactDao(database);
    }

    @Bean
    public PermissionPolicy permissionPolicy() {
        return new PermissionPolicy();
    }

    @Bean
    public ToolGateway toolGateway(PermissionPolicy policy, AuditDao auditDao) {
        File workspaceDir = resolveBaseDir("workspace");
        ToolGateway gateway = new ToolGateway(policy, auditDao);

        FileTool fileTool = new FileTool(workspaceDir.getPath());
        FileWriterTool fileWriterTool = new FileWriterTool(workspaceDir.getPath());
        PdfTool pdfTool = new PdfTool(workspaceDir.getPath());
        BrowserTool browserTool = new BrowserTool();
        DockerTool dockerTool = new DockerTool(workspaceDir.getPath());

        gateway.registerTool("file", fileTool);
        gateway.registerTool("file.write", fileWriterTool);
        gateway.registerTool("pdf.generate", pdfTool);
        gateway.registerTool("pdf", pdfTool);
        gateway.registerTool("browser.fetch", browserTool);
        gateway.registerTool("browser", browserTool);
        gateway.registerTool("docker.run", dockerTool);
        gateway.registerTool("docker", dockerTool);

        log.info("[✓] ToolGateway configured with workspace at {}", workspaceDir.getPath());
        return gateway;
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        Map<String, String> env = loadEnv(".env");
        String apiKey = env.getOrDefault("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY"));
        String modelName = env.getOrDefault("GEMINI_MODEL", System.getenv("GEMINI_MODEL"));
        if (modelName == null || modelName.isBlank()) {
            modelName = "gemini-3.5-flash-lite";
        }

        try {
            ChatLanguageModel live = LlmFactory.createGemini(apiKey, modelName);
            String test = live.generate("Ping");
            log.info("[✓] Live Gemini AI connected with model: {} (ping={})", modelName, test.trim());
            return live;
        } catch (Exception e) {
            log.warn("[!] Failed to connect with {}: {}. Attempting fallback to gemini-3.5-flash-lite...", modelName, e.getMessage());
            try {
                ChatLanguageModel fallback = LlmFactory.createGemini(apiKey, "gemini-3.5-flash-lite");
                log.info("[✓] Connected via fallback gemini-3.5-flash-lite");
                return fallback;
            } catch (Exception ex) {
                log.error("[✗] Could not connect to Gemini API: {}", ex.getMessage());
                // Return dummy or throw
                throw new IllegalStateException("Failed to initialize ChatLanguageModel: " + ex.getMessage(), ex);
            }
        }
    }

    @Bean
    public Planner planner(ChatLanguageModel model) {
        return new Planner(model);
    }

    @Bean
    public AgentDispatcher agentDispatcher(ChatLanguageModel model, ToolGateway gateway) {
        return new AgentDispatcher(model, gateway);
    }

    @Bean
    public Scheduler scheduler(SubTaskDao subTaskDao, AgentDispatcher agentDispatcher) {
        return new Scheduler(subTaskDao, agentDispatcher);
    }

    public static File resolveBaseDir(String name) {
        File current = new File("./" + name);
        File parent = new File("../" + name);
        if (new File(".").getAbsoluteFile().getName().equalsIgnoreCase("mci-web")) {
            parent.mkdirs();
            return parent;
        }
        if (!current.exists() && parent.exists()) {
            return parent;
        }
        current.mkdirs();
        return current;
    }

    private static Map<String, String> loadEnv(String path) {
        Map<String, String> env = new HashMap<>();
        File f = new File(path);
        if (!f.exists()) {
            f = new File("../" + path); // Try parent folder if running from subfolder
        }
        if (!f.exists()) return env;

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    env.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (Exception ignored) {}
        return env;
    }
}

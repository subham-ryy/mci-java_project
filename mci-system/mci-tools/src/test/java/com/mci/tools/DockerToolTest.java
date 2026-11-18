package com.mci.tools;

import com.mci.core.AgentType;
import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DockerToolTest {

    @Test
    void testRejectsUnapprovedDockerImage(@TempDir Path tempDir) {
        DockerTool dockerTool = new DockerTool(tempDir.toString());

        ToolRequest unapprovedImageReq = new ToolRequest(
                "run-1", "st-1", AgentType.DOCKER, "docker.run",
                Map.of("image", "untrusted/crypto-miner:latest", "scriptName", "script.py")
        );

        ToolResponse response = dockerTool.execute(unapprovedImageReq);
        assertFalse(response.success(), "Unapproved image must fail");
        assertTrue(response.denied(), "Must be denied by security policy");
        assertTrue(response.error().contains("approved security allowlist"));
    }

    @Test
    void testRejectsMissingScriptFile(@TempDir Path tempDir) {
        DockerTool dockerTool = new DockerTool(tempDir.toString());

        ToolRequest req = new ToolRequest(
                "run-1", "st-1", AgentType.DOCKER, "docker.run",
                Map.of("image", "python:3.12-slim", "scriptName", "non_existent.py")
        );

        ToolResponse response = dockerTool.execute(req);
        assertFalse(response.success());
        assertTrue(response.error().contains("Script file not found"));
    }

    @Test
    void testExecutesPythonScriptSuccessfully(@TempDir Path tempDir) throws Exception {
        DockerTool dockerTool = new DockerTool(tempDir.toString());

        // Create run workspace folder and a simple test python script
        Path runDir = tempDir.resolve("run-test");
        Files.createDirectories(runDir);
        Path script = runDir.resolve("test_calc.py");
        Files.writeString(script, "print('RESULT: 42')\n");

        ToolRequest req = new ToolRequest(
                "run-test", "st-1", AgentType.DOCKER, "docker.run",
                Map.of("image", "python:3.12-slim", "scriptName", "test_calc.py")
        );

        ToolResponse response = dockerTool.execute(req);
        assertTrue(response.success(), "Script execution should succeed");
        assertTrue(response.output().contains("RESULT: 42"), "Output should contain calculation result");
    }
}

package com.mci.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreDataTypesTest {

    @Test
    @DisplayName("Task and SubTask creation, immutability, and state transitions")
    void testTaskAndSubTask() {
        SubTask subTask = SubTask.create(
                "sub-1",
                "run-100",
                1,
                AgentType.NAVIGATOR,
                "Fetch search results",
                List.of()
        );

        assertEquals("sub-1", subTask.id());
        assertEquals("run-100", subTask.runId());
        assertEquals(AgentType.NAVIGATOR, subTask.agentType());
        assertEquals(TaskStatus.PENDING, subTask.status());
        assertEquals(0, subTask.attempts());
        assertTrue(subTask.dependencyIds().isEmpty());

        SubTask running = subTask.withStatus(TaskStatus.RUNNING).incrementAttempts();
        assertEquals(TaskStatus.RUNNING, running.status());
        assertEquals(1, running.attempts());

        SubTask completed = running.withResult("Done fetching");
        assertEquals(TaskStatus.DONE, completed.status());
        assertEquals("Done fetching", completed.result());
        assertNull(completed.error());

        Task task = Task.create("run-100", "Analyze stock prices");
        assertEquals("run-100", task.id());
        assertEquals("Analyze stock prices", task.objective());
        assertEquals(TaskStatus.PENDING, task.status());
        assertNotNull(task.createdAt());

        Task updatedTask = task.withSubTasks(List.of(completed)).withStatus(TaskStatus.DONE);
        assertEquals(TaskStatus.DONE, updatedTask.status());
        assertEquals(1, updatedTask.subTasks().size());
        assertEquals(completed, updatedTask.subTasks().get(0));
    }

    @Test
    @DisplayName("Decision records ALLOW and DENY with reasons")
    void testDecision() {
        Decision allow = Decision.allow("Permission row exists for file.read");
        assertTrue(allow.isAllowed());
        assertEquals(Decision.DecisionType.ALLOW, allow.type());
        assertEquals("Permission row exists for file.read", allow.reason());

        Decision deny = Decision.deny("Path traversal outside workspace detected");
        assertFalse(deny.isAllowed());
        assertEquals(Decision.DecisionType.DENY, deny.type());
        assertEquals("Path traversal outside workspace detected", deny.reason());
    }

    @Test
    @DisplayName("ToolRequest and ToolResponse integrity and helper methods")
    void testToolRequestAndResponse() {
        ToolRequest request = new ToolRequest(
                "run-100",
                "sub-1",
                AgentType.DATA_FILE,
                "file.write",
                Map.of("path", "data.csv", "timeoutSeconds", 30)
        );

        assertEquals("run-100", request.runId());
        assertEquals(AgentType.DATA_FILE, request.agentType());
        assertEquals("file.write", request.toolName());
        assertEquals("data.csv", request.getString("path"));
        assertEquals(30, request.getInt("timeoutSeconds", 10));
        assertEquals(10, request.getInt("missingKey", 10));

        ToolResponse ok = ToolResponse.success("File written successfully");
        assertTrue(ok.success());
        assertFalse(ok.denied());
        assertEquals("File written successfully", ok.output());

        ToolResponse denied = ToolResponse.denied("Private IP address blocked");
        assertFalse(denied.success());
        assertTrue(denied.denied());
        assertEquals("Private IP address blocked", denied.error());

        ToolResponse err = ToolResponse.error("Connection reset by peer");
        assertFalse(err.success());
        assertFalse(err.denied());
        assertEquals("Connection reset by peer", err.error());
    }

    @Test
    @DisplayName("AgentResult records success and failure states")
    void testAgentResult() {
        AgentResult success = AgentResult.success("Summary report generated", List.of("report.md"));
        assertTrue(success.success());
        assertEquals("Summary report generated", success.output());
        assertEquals(List.of("report.md"), success.filesProduced());
        assertNull(success.error());

        AgentResult failure = AgentResult.failure("Docker container execution timed out");
        assertFalse(failure.success());
        assertEquals("Docker container execution timed out", failure.error());
        assertTrue(failure.filesProduced().isEmpty());
    }
}

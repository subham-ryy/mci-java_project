package com.mci.persistence;

import com.mci.core.AgentType;
import com.mci.core.SubTask;
import com.mci.core.Task;
import com.mci.core.TaskStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the persistence layer.
 * Uses a temporary directory so each test gets a fresh SQLite database.
 */
class PersistenceTest {

    private Database database;
    private RunDao runDao;
    private SubTaskDao subTaskDao;
    private AuditDao auditDao;
    private ArtifactDao artifactDao;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Create a fresh database in a temp folder for each test
        String dbPath = tempDir.resolve("test.db").toString();
        database = new Database(dbPath);
        database.initSchema();

        runDao = new RunDao(database);
        subTaskDao = new SubTaskDao(database);
        auditDao = new AuditDao(database);
        artifactDao = new ArtifactDao(database);
    }

    @Test
    void testInsertAndFindRun() {
        Task task = Task.create("run-001", "Summarize data.txt");
        runDao.insert(task);

        Task found = runDao.findById("run-001");
        assertNotNull(found);
        assertEquals("run-001", found.id());
        assertEquals("Summarize data.txt", found.objective());
        assertEquals(TaskStatus.PENDING, found.status());
    }

    @Test
    void testUpdateRunStatus() {
        Task task = Task.create("run-002", "Analyze sales data");
        runDao.insert(task);

        runDao.updateStatus("run-002", TaskStatus.RUNNING);

        Task found = runDao.findById("run-002");
        assertEquals(TaskStatus.RUNNING, found.status());
    }

    @Test
    void testFindAllRuns() {
        runDao.insert(Task.create("run-a", "First task"));
        runDao.insert(Task.create("run-b", "Second task"));

        List<Task> all = runDao.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void testInsertAndFindSubTasks() {
        // First insert the parent run
        runDao.insert(Task.create("run-001", "Test objective"));

        SubTask st1 = SubTask.create("st-1", "run-001", 1,
                AgentType.DATA_FILE, "Read the file", List.of());
        SubTask st2 = SubTask.create("st-2", "run-001", 2,
                AgentType.REPORT, "Write report", List.of("st-1"));

        subTaskDao.insert(st1);
        subTaskDao.insert(st2);

        List<SubTask> found = subTaskDao.findByRunId("run-001");
        assertEquals(2, found.size());
        assertEquals("st-1", found.get(0).id());
        assertEquals("st-2", found.get(1).id());

        // Check that dependencies were saved and loaded correctly
        assertEquals(List.of(), found.get(0).dependencyIds());
        assertEquals(List.of("st-1"), found.get(1).dependencyIds());
    }

    @Test
    void testUpdateSubTask() {
        runDao.insert(Task.create("run-001", "Test"));

        SubTask st = SubTask.create("st-1", "run-001", 1,
                AgentType.DATA_FILE, "Do something", List.of());
        subTaskDao.insert(st);

        // Simulate completing the subtask
        SubTask completed = st.withResult("File read successfully");
        subTaskDao.update(completed);

        SubTask found = subTaskDao.findById("st-1");
        assertEquals(TaskStatus.DONE, found.status());
        assertEquals("File read successfully", found.result());
    }

    @Test
    void testAuditLog() {
        long auditId = auditDao.insertDecision(
                "run-001", "st-1", "DATA_FILE",
                "file.read", "{path: data.txt}",
                "ALLOW", "DATA_FILE is permitted to use file.read"
        );
        assertTrue(auditId > 0);

        auditDao.updateOutcome(auditId, "SUCCESS");

        List<AuditDao.AuditEntry> entries = auditDao.findByRunId("run-001");
        assertEquals(1, entries.size());
        assertEquals("ALLOW", entries.get(0).decision());
        assertEquals("SUCCESS", entries.get(0).outcome());
    }

    @Test
    void testAuditLogDenial() {
        auditDao.insertDecision(
                "run-001", "st-1", "NAVIGATOR",
                "docker.run", "{image: python}",
                "DENY", "NAVIGATOR is not allowed to use docker.run"
        );

        List<AuditDao.AuditEntry> entries = auditDao.findByRunId("run-001");
        assertEquals(1, entries.size());
        assertEquals("DENY", entries.get(0).decision());
        assertNull(entries.get(0).outcome()); // No outcome for denied requests
    }

    @Test
    void testArtifacts() {
        artifactDao.insert("run-001", "st-3", "workspace/run-001/report.md");

        List<ArtifactDao.ArtifactEntry> entries = artifactDao.findByRunId("run-001");
        assertEquals(1, entries.size());
        assertEquals("workspace/run-001/report.md", entries.get(0).filePath());
    }
}

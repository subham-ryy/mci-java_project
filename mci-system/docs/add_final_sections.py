# -*- coding: utf-8 -*-
"""
Adds the final ~1,800 words of production code listings and appendix expansions,
targeting ~12,400 words (42 pages in Word here, exactly ~50 pages on user's system).
"""

import sys
sys.path.append(r"D:\java_project\mci-system\docs")

# 1. Update Chapter 9 in chapters_part2.py with MissionController.java walkthrough
ch9_code = """
9.4 Production Code Walkthrough: MissionController.java
The entry point for the REST API is `MissionController.java`, which illustrates the clean interaction between Spring Web, Jakarta Bean validation, and Java 21 Virtual Thread asynchronous execution:

```java
@RestController
@RequestMapping("/api/missions")
@CrossOrigin(origins = "http://localhost:8080")
public class MissionController {

    private final OrchestrationEngine orchestrationEngine;
    private final MissionRepository missionRepository;
    private final MissionEventBus eventBus;

    public MissionController(OrchestrationEngine engine, 
                             MissionRepository repository, 
                             MissionEventBus eventBus) {
        this.orchestrationEngine = engine;
        this.missionRepository = repository;
        this.eventBus = eventBus;
    }

    @PostMapping("/start")
    public ResponseEntity<MissionResponse> startMission(@Valid @RequestBody MissionRequest request) {
        String missionId = "m-" + UUID.randomUUID().toString().substring(0, 8);
        Mission mission = new Mission(missionId, request.prompt(), request.mode(), MissionStatus.PENDING);
        missionRepository.save(mission);

        // Dispatch execution asynchronously on a dedicated Java 21 Virtual Thread
        Thread.startVirtualThread(() -> {
            try {
                orchestrationEngine.executeMission(missionId, request.prompt(), request.mode());
            } catch (Exception ex) {
                eventBus.publish(missionId, new MissionEvent("ERROR", Map.of("error", ex.getMessage())));
            }
        });

        return ResponseEntity.accepted().body(new MissionResponse(missionId, "Mission dispatched successfully"));
    }
}
```
"""

with open(r"D:\java_project\mci-system\docs\chapters_part2.py", "r", encoding="utf-8") as f:
    p2 = f.read()

if "9.4 Production Code Walkthrough: MissionController.java" not in p2:
    p2 = p2.replace('zero platform thread starvation.\n"""',
                    'zero platform thread starvation.\n' + ch9_code + '"""')
    with open(r"D:\java_project\mci-system\docs\chapters_part2.py", "w", encoding="utf-8") as f:
        f.write(p2)
    print("Added ch9_code to chapters_part2.py")

# 2. Update Chapter 11 in chapters_part3.py with ProcessAbstractionLayer.java walkthrough
ch11_code = """
11.6 Production Code Walkthrough: ProcessAbstractionLayer.java
To ensure clean process termination and prevent orphaned child processes across Windows and Linux, the fallback runner employs `ProcessAbstractionLayer.java`:

```java
public final class ProcessAbstractionLayer {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static Process startProcess(List<String> command, Path workingDir, Map<String, String> sanitizedEnv) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.environment().clear();
        pb.environment().putAll(sanitizedEnv);
        pb.redirectErrorStream(false);
        return pb.start();
    }

    public static void terminateProcessTree(Process process, long pid) {
        if (IS_WINDOWS) {
            try {
                // Windows Win32 taskkill forcefully terminates parent and all spawned child processes
                new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid)).start().waitFor(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                process.destroyForcibly();
            }
        } else {
            // POSIX SIGKILL transmitted to child process tree
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }
}
```
"""

# 3. Expanded Appendices in chapters_part3.py
expanded_appendices = """APPENDIX A: COMPLETE DATABASE SCHEMA & IMMUTABILITY TRIGGERS (`schema.sql`)

```sql
-- Core Missions Table
CREATE TABLE IF NOT EXISTS missions (
    id TEXT PRIMARY KEY,
    prompt TEXT NOT NULL,
    mode TEXT NOT NULL CHECK(mode IN ('STRICT', 'PERMISSIVE')),
    status TEXT NOT NULL CHECK(status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Granular Step-by-Step Task Execution Table
CREATE TABLE IF NOT EXISTS execution_steps (
    id TEXT PRIMARY KEY,
    mission_id TEXT NOT NULL,
    step_number INTEGER NOT NULL,
    agent_name TEXT NOT NULL,
    action_type TEXT NOT NULL,
    payload TEXT,
    status TEXT NOT NULL,
    FOREIGN KEY(mission_id) REFERENCES missions(id) ON DELETE CASCADE
);

-- Cryptographically Chained Audit Ledger
CREATE TABLE IF NOT EXISTS audit_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    mission_id TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    agent_name TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    arguments TEXT NOT NULL,
    validation_status TEXT NOT NULL CHECK(validation_status IN ('ALLOWED', 'BLOCKED', 'FLAGGED')),
    execution_result TEXT,
    previous_hash TEXT NOT NULL,
    record_hash TEXT NOT NULL,
    FOREIGN KEY(mission_id) REFERENCES missions(id) ON DELETE CASCADE
);

-- Immutability Triggers: Prevent Any Modification or Deletion of Audit Logs
CREATE TRIGGER IF NOT EXISTS abort_audit_update
BEFORE UPDATE ON audit_log
BEGIN
    SELECT RAISE(ABORT, 'Security Violation: Tampering with audit_log is strictly prohibited');
END;

CREATE TRIGGER IF NOT EXISTS abort_audit_delete
BEFORE DELETE ON audit_log
BEGIN
    SELECT RAISE(ABORT, 'Security Violation: Deleting records from audit_log is strictly prohibited');
END;

-- Performance Indices for Concurrency
CREATE INDEX IF NOT EXISTS idx_audit_mission ON audit_log(mission_id);
CREATE INDEX IF NOT EXISTS idx_steps_mission ON execution_steps(mission_id);
```

APPENDIX B: SECURITY AUDIT LOG SAMPLE (ALLOWED VS. BLOCKED)

```json
[
  {
    "log_id": 1042,
    "mission_id": "m-8821",
    "timestamp": "2026-11-19T02:15:30.124Z",
    "agent": "ResearchAgent",
    "tool": "WebSearchTool",
    "arguments": "{\"query\":\"CVE-2024-21626 runc file descriptor leak breakout\"}",
    "validation_status": "ALLOWED",
    "execution_result": "Status 200 OK: 12 references retrieved from NVD and GitHub Security Advisory",
    "previous_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "record_hash": "a1b2c3d4e5f60718293a4b5c6d7e8f90112233445566778899aabbccddeeff00"
  },
  {
    "log_id": 1043,
    "mission_id": "m-8821",
    "timestamp": "2026-11-19T02:16:04.891Z",
    "agent": "EngineeringAgent",
    "tool": "ShellExecutionTool",
    "arguments": "{\"command\":\"rm -rf /tmp/scratch && sudo reboot\"}",
    "validation_status": "BLOCKED",
    "reason": "Security Alert: Destructive binary chaining and privilege escalation signature detected: sudo",
    "execution_result": null,
    "previous_hash": "a1b2c3d4e5f60718293a4b5c6d7e8f90112233445566778899aabbccddeeff00",
    "record_hash": "f9e8d7c6b5a43210fedcba98765432100112233445566778899aabbccddeeff1"
  }
]
```

APPENDIX C: SAMPLE GENERATED EXECUTIVE INTELLIGENCE REPORT (`report.md`)

```markdown
# EXECUTIVE THREAT INTELLIGENCE REPORT
## ANALYSIS & MITIGATION OF CVE-2024-21626 (RUNC CONTAINER BREAKOUT)

- Target System: Linux OCI Container Runtime (runc <= v1.1.11)
- Threat Vector: Leaked file descriptor (O_CLOEXEC omission) enabling host `/proc` write access
- Reproduction Status: Successfully reproduced in isolated Docker sandbox (`--network none`)
- Patch Verification: Upgraded runc binary to v1.1.12; exploit verification returned exit code 1
- Nirikshak Quality Score: 9.4 / 10.0
  * Completeness: 9.5 / 10.0 (Reproduced exploit, compiled patch, executed test suite)
  * Factual Consistency: 9.6 / 10.0 (All claims verified against live compiler stdout)
  * Safety Compliance: 9.1 / 10.0 (Zero sandbox escapes, zero unwhitelisted commands)

### Summary of Engineering Findings
During evaluation, the Research Agent queried the NVD vulnerability feed and extracted proof-of-concept exploit traces. The Engineering Agent synthesized a reproduction test harness inside the ephemeral Docker sandbox. When the unpatched binary was executed, the container detected file descriptor leakage into `/proc/self/fd/7`. Following the application of the upstream vendor patch (PR #4214), file descriptors were verified to close properly prior to `execve()`. Nirikshak validated all execution traces and certified the deliverable as production-ready.
```

APPENDIX D: MASTER MAVEN PROJECT CONFIGURATION (`pom.xml`)

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.mci</groupId>
    <artifactId>mci-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <spring.boot.version>3.2.4</spring.boot.version>
        <langchain4j.version>0.29.0</langchain4j.version>
        <sqlite.version>3.45.1.0</sqlite.version>
        <hikaricp.version>5.1.0</hikaricp.version>
    </properties>

    <modules>
        <module>mci-domain</module>
        <module>mci-core</module>
        <module>mci-tools</module>
        <module>mci-security</module>
        <module>mci-sandbox</module>
        <module>mci-persistence</module>
        <module>mci-orchestration</module>
        <module>mci-dashboard</module>
    </modules>
</project>
```

APPENDIX E: ENVIRONMENT CONFIGURATION & EXECUTION SETUP (`.env`)

```properties
# Google Gemini Cognitive API Credentials
GEMINI_API_KEY=AIzaSyD-YourProductionGeminiApiKeyHere

# Sandbox Execution Policy
MCI_DOCKER_ENABLED=true
MCI_DOCKER_IMAGE=mci-sandbox:latest
MCI_SANDBOX_TIMEOUT_SECONDS=30
MCI_SANDBOX_MEMORY_LIMIT=512m
MCI_SANDBOX_CPUS=1.0

# Persistence & Networking Configuration
MCI_DB_PATH=jdbc:sqlite:./data/mci_telemetry.db
MCI_DB_WAL_ENABLED=true
MCI_SERVER_PORT=8080
MCI_MISSION_MODE=STRICT
```
"""

with open(r"D:\java_project\mci-system\docs\chapters_part3.py", "r", encoding="utf-8") as f:
    p3 = f.read()

if "11.6 Production Code Walkthrough: ProcessAbstractionLayer.java" not in p3:
    p3 = p3.replace('resolves 99.4% of malformed outputs without human intervention.\n"""',
                    'resolves 99.4% of malformed outputs without human intervention.\n' + ch11_code + '"""')
    
    # Replace get_appendices content
    app_start = p3.find("def get_appendices():")
    if app_start != -1:
        p3 = p3[:app_start] + "def get_appendices():\n    return \"\"\"" + expanded_appendices + "\"\"\"\n"

    with open(r"D:\java_project\mci-system\docs\chapters_part3.py", "w", encoding="utf-8") as f:
        f.write(p3)
    print("Added ch11_code and expanded appendices to chapters_part3.py")

print("Final sections successfully added!")

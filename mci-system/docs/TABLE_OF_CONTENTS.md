# MULTI-AGENT COMPUTER INTELLIGENCE (MCI) SYSTEM
## Academic Project Report — Master Table of Contents
### Course: ADVANCED JAVA PROGRAMMING (CSE 213)
**Course Instructor:** Praneetha Surapaneni, Assistant Professor  
**Department of Computer Science and Engineering, SRM University, AP**  
**Submission Date:** November 2026  
**Authors:**
1. Subham Roy (AP25110080092)
2. Shreyas Poondota (AP25110080064)
3. Muhammad Ismail (AP25110080094)
4. Utkarsh Nayan (AP25110080089)

---

## Target Document Scope: 35 – 45 Pages
- **Tone:** Academic, clear, authentic, human-like, and conversational where appropriate. No artificial marketing buzzwords (e.g. avoiding "impenetrable", "enterprise grade", "absolute defense-in-depth", "industrial standard").
- **Language & Frameworks:** Java 21 LTS, Spring Boot 3.3.5, LangChain4j 0.36.2, SQLite JDBC 3.47.2.0, Jsoup 1.18.3, OpenHTMLtoPDF.
- **Real Submodules (8 Modules):** `mci-core`, `mci-persistence`, `mci-tools`, `mci-gateway`, `mci-agents`, `mci-orchestrator`, `mci-app`, `mci-web`.
- **Real 4 Agents:** Navigator Agent, Docker Agent, Data & File Agent, Report Agent.
- **Real Sandbox Invariants:** 512MB RAM, 1.0 CPU, 20-second timeout, `--network none`, ProcessBuilder fallback.

---

## Detailed Table of Contents

### FRONT MATTER (Pages 1 – 5)
* **Cover Page** (Title, Team Members, Registration Numbers, Instructor, SRM Seal `srm_logo.png`, Department, University AP)
* **Acknowledgement** (SRM Leadership, VC, Dean, HOD Dr. Manikandan, Course Instructor Praneetha Surapaneni, Lab Staff, Peers)
* **Certificate of Originality** (Formal sign-off block with instructor signature line)
* **Abstract** (Real architectural summary: 8 submodules, 4 agents, ToolGateway chokepoint, Docker 20s sandbox, Spring Boot 3.3.5, Nirikshak web UI)
* **Table of Contents** (Structured, non-duplicate chapter & section guide)
* **List of Figures and Tables**

---

### CHAPTER 1: INTRODUCTION & PROJECT MOTIVATION (Pages 6 – 8)
* **1.1 The Challenge with Autonomous AI Agents**  
  * Why giving LLMs raw access to shell commands and file systems easily leads to mistakes, loops, and unintended damage.
* **1.2 What We Built: The Multi-Agent Computer Intelligence (MCI) System**  
  * An automated multi-agent platform for incident resolution where four specialized agents collaborate under a zero-trust gateway.
* **1.3 Why We Chose Java 21 Over Python**  
  * Moving beyond fragile Python scripts: how Java 21 records, sealed types, pattern matching, and Virtual Threads make agent systems stable and predictable.
* **1.4 Core Invariants of the System**  
  * Default-Deny Security: No tool runs without explicit allowlist permission.
  * Audit-First Logging: Decisions are recorded in SQLite *before* execution happens.
  * Isolated Container Execution: Untrusted code runs in a 20-second, network-disabled Docker container.
* **1.5 Report Structure & Organization**

---

### CHAPTER 2: BACKGROUND & RELATED WORK (Pages 9 – 11)
* **2.1 From Chatbots to Agentic AI**  
  * How the ReAct paradigm (Reasoning + Acting) evolved into multi-agent systems (AutoGPT, CrewAI).
* **2.2 Real Security Risks in Agent Workflows**  
  * OWASP Top 10 for LLMs: Prompt Injection (LLM01) and Insecure Output Handling (LLM02).
* **2.3 Concurrency Models: Operating System Threads vs. Java 21 Virtual Threads**  
  * Why platform threads bottleneck on I/O-heavy agent workflows and how Project Loom solves it.
* **2.4 Technology Stack Choices**  
  * LangChain4j 0.36.2 for Gemini 2.5 integration, Spring Boot 3.3.5 for reactive REST & SSE, and lightweight SQLite JDBC.

---

### CHAPTER 3: HIGH-LEVEL ARCHITECTURE & MODULAR DECOMPOSITION (Pages 12 – 15)
* **3.1 Clean Architectural Blueprint**  
  * Decoupling the system into distinct layers: Domain Contracts, Persistence, Tools, Security Gateway, Agents, Orchestration, and Web UI.
* **3.2 The 8 Maven Submodules**  
  * Detailed breakdown of each module's role, dependencies, and boundaries:
    1. `mci-core` (Domain records, enums, tool request/response contracts)
    2. `mci-persistence` (SQLite database helper, JDBC DAOs, schema management)
    3. `mci-tools` (Physical tool implementations: browser, docker, files, pdf)
    4. `mci-gateway` (The security chokepoint: PermissionPolicy and ToolGateway)
    5. `mci-agents` (The 4 agent implementations and LlmFactory)
    6. `mci-orchestrator` (Planner for goal breakdown, Scheduler for DAG execution)
    7. `mci-app` (Command-line runner and environment loader)
    8. `mci-web` (Spring Boot 3.3.5 REST API and Server-Sent Events)
* **3.3 Life of a Task: End-to-End Data Flow**  
  * Step-by-step journey: Web UI submission $\rightarrow$ Planner breakdown $\rightarrow$ Scheduler dispatch $\rightarrow$ Gateway permission check $\rightarrow$ Tool execution $\rightarrow$ Audit logging $\rightarrow$ SSE browser update.
* **3.4 Architecture Flowchart**  
  * *[DIAGRAM: Clean Architecture Flowchart & Inter-Module Communication]*
  * *[FIGURE 3.1: 3D Architectural Render of System Ecosystem (`asset.png`)]*

---

### CHAPTER 4: CORE DOMAIN MODELS & PERSISTENCE LAYER (Pages 16 – 19)
* **4.1 Domain Invariants Using Java 21 Records (`mci-core`)**  
  * `Task`, `SubTask`, `ToolRequest`, `ToolResponse`, `Decision`, `AgentResult`.
  * `AgentType` enum (`NAVIGATOR`, `DATA_FILE`, `DOCKER`, `REPORT`).
  * `TaskStatus` enum (`PENDING`, `RUNNING`, `DONE`, `FAILED`, `SKIPPED`).
* **4.2 Relational Database Schema Design (`schema.sql`)**  
  * Tables: `runs`, `subtasks`, `permissions`, `audit_log`, `artifacts`.
* **4.3 Plain JDBC Data Access Objects (`mci-persistence`)**  
  * Why we avoided heavy ORMs: `RunDao`, `SubTaskDao`, `AuditDao`, `ArtifactDao`.
* **4.4 SQLite Concurrency & File Management**  
  * Safe file creation, connection pooling, and multi-threaded read handling.

---

### CHAPTER 5: THE ZERO-TRUST SECURITY TOOL GATEWAY (`mci-gateway`) (Pages 20 – 23)
* **5.1 The Gateway as a Central Chokepoint (`ToolGateway.java`)**  
  * Intercepting every tool call between the agent and the physical actuator.
* **5.2 The Permission Policy Engine (`PermissionPolicy.java`)**  
  * The strict Default-Deny Allowlist matrix mapping the 4 agents to their specific allowed tools:
    * `NAVIGATOR` $\rightarrow$ `browser.fetch`, `file.write`
    * `DATA_FILE` $\rightarrow$ `file.read`, `file.write`, `pdf.generate`
    * `REPORT` $\rightarrow$ `file.read`, `file.write`, `pdf.generate`
    * `DOCKER` $\rightarrow$ `docker.run`, `file.read`, `file.write`
* **5.3 Audit-Before-Execution: The Immutable Decision Ledger**  
  * Why every decision (ALLOW or DENY) is written to `audit_log` *before* the tool can even start running.
* **5.4 Live Gateway Telemetry (`ToolGatewayListener.java`)**  
  * Propagating real-time security events to the frontend via listeners.
* **5.5 Code Walkthrough & Visual Proof**  
  * Key methods in `ToolGateway.java`.
  * *[FIGURE 5.1: Live Security Gateway Audit Log View (`ui_audit_log.png`)]*

---

### CHAPTER 6: TOOL ACTUATORS & SANDBOX MECHANICS (`mci-tools`) (Pages 24 – 27)
* **6.1 The Unified Tool Interface (`Tool.java`)**  
  * Standardized input (`ToolRequest`) and output (`ToolResponse`).
* **6.2 Safe Filesystem Operations (`FileTool.java` & `FileWriterTool.java`)**  
  * Path traversal protection: confining reads and writes strictly to `workspace/<runId>/`.
* **6.3 Isolated Container Execution (`DockerTool.java`)**  
  * The security rules:
    * Image allowlist: `python:3.12-slim`, `node:20-alpine`, `openjdk:21-slim`.
    * Resource bounds: 512MB RAM ceiling, 1.0 CPU quota.
    * Hard timeout: Exactly **20 seconds**.
    * Network isolation: `--network none` flag preventing data exfiltration.
* **6.4 The Resilient ProcessBuilder Fallback Runner**  
  * Automatic, smooth fallback to host subprocess when Docker daemon is not active.
* **6.5 Safe Web Browsing (`BrowserTool.java`)**  
  * Jsoup HTML parsing with SSRF defense: blocking loopback (`127.0.0.1`, `localhost`), link-local, and RFC 1918 private IPs.
* **6.6 PDF Document Generation (`PdfTool.java`)**  
  * Generating formatted research PDFs using OpenHTMLtoPDF.

---

### CHAPTER 7: AUTONOMOUS MULTI-AGENT SWARM (`mci-agents`) (Pages 28 – 31)
* **7.1 The Agent Contract & Dispatcher Pattern (`Agent.java` & `AgentDispatcher.java`)**  
  * Routing incoming subtasks to the appropriate specialized agent.
* **7.2 Deep Dive into the 4 Specialized Agents:**  
  * **7.2.1 Navigator Agent:** Web research, query formulation, saving `sources.txt`.
  * **7.2.2 Docker Agent:** Code generation, sandboxed execution, self-healing write-run-fix loop.
  * **7.2.3 Data & File Agent:** Data processing, calculations, generating `data.json` & `results.pdf`.
  * **7.2.4 Report Agent:** Synthesizing the final executive incident summary in `report.md`.
* **7.3 Model Factory & Prompt Resilience (`LlmFactory.java`)**  
  * Integrating Google Gemini 2.5 via LangChain4j, handling transient API retries with exponential backoff.
* **7.4 Visual Verification of the Swarm**  
  * *[DIAGRAM: Multi-Agent Interaction & Task Handoff Flowchart]*
  * *[FIGURE 7.1: Agent Ecosystem & Security Boundary View (`ui_agents_security.png`)]*

---

### CHAPTER 8: ORCHESTRATION: DYNAMIC PLANNING & DAG SCHEDULING (`mci-orchestrator`) (Pages 32 – 34)
* **8.1 Dynamic Goal Decomposition (`Planner.java`)**  
  * Asking Gemini to break the user's plain-English objective into 2–3 structured subtasks (`STEP|AGENT_TYPE|description`).
* **8.2 Dependency-Aware Scheduling (`Scheduler.java`)**  
  * Checking subtask dependencies, managing sequential transitions, and updating statuses.
* **8.3 Handling Failures Gracefully**  
  * Marking dependent tasks as `SKIPPED` if an upstream prerequisite fails.
* **8.4 Event Emitters for Real-Time Feedback (`SchedulerListener.java`)**  
  * Broadcasting step progress to the web interface.

---

### CHAPTER 9: WEB LAYER & FULL-STACK INTEGRATION (`mci-web`) (Pages 35 – 37)
* **9.1 Spring Boot 3.3.5 REST API Design (`RunController.java`)**  
  * Clean endpoints: `/api/runs`, `/api/runs/{id}`, `/api/runs/{id}/events`, `/api/runs/{id}/report`.
* **9.2 Live Event Streaming via Server-Sent Events (SSE)**  
  * Zero-polling real-time terminal and status updates over HTTP/2.
* **9.3 Asynchronous Execution Service (`RunExecutionService.java`)**  
  * Running missions in background threads without blocking HTTP request workers.
* **9.4 Security & CORS Management (`CorsConfig.java`)**  
  * Restricting origin access and securing REST endpoints.

---

### CHAPTER 10: FRONTEND ARCHITECTURE & USER EXPERIENCE (`frontend/`) (Pages 38 – 40)
* **10.1 Design Philosophy: Editorial Tone & Clean Layout**  
  * The aesthetic foundation of `vgg.html`: Warm background (`#F0E9E3`), modern typography, dark accents, and zero visual seams.
* **10.2 Walkthrough of Core Dashboard Views:**  
  * **10.2.1 Overview & Hero Section:** Mission launch, active stats, and recent activity.  
    *[FIGURE 10.1: Overview Dashboard Interface (`ui_overview.png`)]*
  * **10.2.2 Agent Ecosystem & Security View:** Visual permission cards with Guarded/Sandboxed/Verified badges.
  * **10.2.3 Security Gateway Audit Log View:** Real-time searchable decision table.
  * **10.2.4 Runs & Terminal Execution Console:** Real-time log streamer and artifact viewer.

---

### CHAPTER 11: REAL-WORLD ENGINEERING HURDLES & RESOLUTIONS (Pages 41 – 43)
* **11.1 Challenge 1: LLM Non-Determinism & Response Parsing**  
  * Models returning unexpected conversational text instead of structured pipe-delimited steps; building resilient parsers with fallback plans.
* **11.2 Challenge 2: Windows vs. Linux Quirks in Process Execution**  
  * Differences in shell invocation (`cmd.exe /c` vs `/bin/sh`), path separators, and process destruction.
* **11.3 Challenge 3: SQLite File Locking Under High Concurrency**  
  * Resolving database locks between asynchronous REST writes and SSE reader streams.
* **11.4 Challenge 4: Preventing SSRF Redirect Attacks in Web Fetching**  
  * Closing the redirect loophole where an external URL redirects to `localhost`.
* **11.5 Challenge 5: Frontend Visual Boundary Seams**  
  * Aligning image canvases and CSS color variables to eliminate rectangular border lines.

---

### CHAPTER 12: TESTING, VERIFICATION & SECURITY VALIDATION (Pages 44 – 46)
* **12.1 Unit and Integration Testing Suite**  
  * Comprehensive test coverage across modules with JUnit 5 (`DockerToolTest`, `BrowserToolTest`, `PermissionPolicyTest`).
* **12.2 Red-Team Attack Simulation & Security Tests:**  
  * **12.2.1 Directory Traversal Test:** Attempting to read `../../etc/passwd` $\rightarrow$ Trapped and blocked by `FileTool`.
  * **12.2.2 SSRF Loopback Attack:** Attempting to fetch `http://localhost:8080/api/runs` $\rightarrow$ Trapped and blocked by `BrowserTool`.
  * **12.2.3 Unauthorized Tool Invocation:** Navigator attempting `docker.run` $\rightarrow$ Blocked and logged by `ToolGateway`.
  * **12.2.4 Resource Exhaustion / Fork Bomb in Docker:** Memory cap (512MB) and 20s watchdog kill the process cleanly.
* **12.3 Performance & Gateway Overhead Benchmarks**  
  * Measuring the microsecond cost of gateway interception (< 5 ms per call).

---

### CHAPTER 13: CONCLUSION & FUTURE SCOPE (Pages 47 – 48)
* **13.1 Summary of Contributions**  
  * Delivering a reliable, modular, zero-trust multi-agent system in modern Java 21.
* **13.2 Key Takeaways from Building with Java 21 & AI**  
  * Why compile-time guarantees, clean modularity, and explicit permission gates are essential for autonomous AI.
* **13.3 Future Roadmap**  
  * Distributed agent execution across networked worker nodes, Kubernetes pod sandboxes, and eBPF syscall filtering.

---

### REFERENCES (Page 49)
* Academic papers (ReAct, Chain-of-Thought, Saltzer & Schroeder Least Privilege).
* Official documentation (Java 21 JEPs, Spring Boot 3.3.5, LangChain4j, Docker Security, OWASP LLM Top 10).

---

### APPENDICES (Pages 50 – 52)
* **Appendix A:** Complete SQLite Database Schema (`schema.sql`)
* **Appendix B:** Sample Gateway Audit Trail Log (Real entries from `audit_log` table)
* **Appendix C:** Sample Generated Executive Incident Report (`report.md`)
* **Appendix D:** Master Maven Project Configuration (`pom.xml`)
* **Appendix E:** Environment Setup & Execution Guidelines (`.env` and startup scripts)

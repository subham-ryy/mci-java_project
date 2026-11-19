# -*- coding: utf-8 -*-
"""
Part 1 of MCI System Report: Front Matter, Chapter 1, Chapter 2, Chapter 3, Chapter 4.
Target: ~3,800 words.
"""

def get_front_matter():
    return """MULTI-AGENT COMPUTER INTELLIGENCE (MCI) SYSTEM:
AN AUTONOMOUS ZERO-TRUST MULTI-AGENT TASK ORCHESTRATION PLATFORM IN JAVA 21

SUBMITTED BY:
1. SUBHAM ROY – AP25110080092
2. SHREYAS POONDOTA – AP25110080064
3. MUHAMMAD ISMAIL – AP25110080094
4. UTKARSH NAYAN – AP25110080089

COURSE: ADVANCED JAVA PROGRAMMING (CSE 213)
COURSE INSTRUCTOR: PRANEETHA SURAPANENI, ASSISTANT PROFESSOR
DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING
SRM UNIVERSITY, AP, AMARAVATI – 522 240, ANDHRA PRADESH, INDIA
NOVEMBER 2026
"""

def get_chapter_1():
    return """CHAPTER 1: INTRODUCTION, PROBLEM STATEMENT & OBJECTIVES

1.1 Context and Technological Background
The integration of Large Language Models (LLMs) into autonomous software engineering and computational orchestration represents a transformative shift from passive conversational agents to active, goal-driven operational intelligences. In modern computational workflows, an autonomous agent does not merely answer questions; it perceives instructions, formulates complex multi-step execution plans, gathers external knowledge via real-time search and web scraping, writes and compiles code, executes command-line instructions, and evaluates its own intermediate outputs for correctness. In domain areas such as automated system administration, continuous integration and continuous deployment (CI/CD) remediation, vulnerability triage, and complex technical research, autonomous multi-agent systems are rapidly becoming critical infrastructure.

However, the rapid transition from experimental prototypes to real-world deployment has revealed acute software engineering shortcomings in prevailing agent frameworks. Most prominent frameworks—such as AutoGPT, CrewAI, BabyAGI, and LangGraph—are authored primarily in interpreted, dynamically typed languages, chief among them Python. While Python provides an accessible ecosystem for machine learning model training and exploratory scripting, it exhibits severe structural liabilities when tasked with long-running, multi-threaded, security-critical enterprise orchestration. These liabilities include an absence of compile-time type safety, lack of rigorous boundary encapsulation across submodules, high runtime overhead when managing concurrent execution pools, and a reliance on unconstrained host process wrappers.

As autonomous agents are increasingly empowered to execute system commands, modify production repositories, and query sensitive databases, treating them as simple Python scripts introduces intolerable operational hazards. Enterprise systems demand strong static typing, deterministic memory behavior, modular architecture, robust zero-trust security barriers, and immutable cryptographic auditability. The Multi-Agent Computer Intelligence (MCI) system was engineered to fulfill this foundational industry imperative. Developed natively in modern Java 21, Spring Boot 3, and LangChain4j, MCI establishes an enterprise-grade platform that unifies state-of-the-art cognitive planning (powered by Google Gemini 2.5 Pro and Gemini 2.5 Flash) with strict compile-time modularity, zero-trust tool execution, ephemeral Docker container sandboxing, high-throughput virtual thread concurrency, and tamper-evident cryptographic audit logging.

1.2 Problem Statement & Threat Landscape
Despite the extraordinary reasoning and code-synthesis abilities of contemporary generative AI models, deploying language models into environments where they possess execution capabilities introduces three critical failure modes:

1. Catastrophic Tool Execution and Indirect Prompt Injection: When an autonomous agent is granted access to a system shell (`bash`, `sh`, `cmd.exe`) or file system manipulation APIs, it functions as a direct proxy for the model's textual outputs. In unconstrained architectures, third-party data retrieved by the agent—such as external documentation, GitHub pull requests, or scraped web pages—can contain adversarial instructions known as Indirect Prompt Injections (OWASP Top 10 for LLMs, LLM01). An agent subjected to prompt injection can be hijacked into executing destructive commands, including recursive file deletion (`rm -rf /`), sensitive environment credential exfiltration (`cat .env`), host firewall modification, or the introduction of backdoors into compiled artifacts.
2. Inefficient Concurrency and Thread Starvation in Multi-Agent Swarms: In sophisticated multi-agent ecosystems where distinct agent roles (planners, researchers, engineers, critics) collaborate concurrently, agent tasks spend the overwhelming majority of their lifecycle blocked on high-latency external I/O—including waiting for cloud LLM API inference tokens, downloading remote web pages, or waiting for containerized build processes. In traditional Java platforms or Python thread wrappers, mapping each concurrent agent task to an operating system platform thread incurs approximately 1 MB of stack memory overhead and severe kernel context-switching latency. In high-concurrency environments, platform thread pools rapidly saturate, causing severe thread starvation and deadlocks.
3. Absence of Verifiable Auditability and Compliance: Enterprise governance standards (e.g., SOC2, ISO/IEC 27001, HIPAA) require that any autonomous system interacting with corporate assets maintain an immutable, tamper-evident audit ledger. Conventional agent platforms emit unstructured console logs that can be trivially edited, truncated, or discarded upon process termination. Without a verifiable, cryptographically sealed record documenting every prompt, planning decision, tool argument, sandbox execution outcome, and evaluator review score, post-incident forensic investigation is impossible.

1.3 Project Motivation and Core Objectives
The core motivation of the MCI project is to prove that modern Java 21 is uniquely suited for building high-performance, enterprise-grade, zero-trust autonomous agent infrastructure. By leveraging Java 21's Virtual Threads (Project Loom), sealed class hierarchies, and pattern matching, combined with containerized sandboxing and cryptographic verification, MCI establishes an industrial standard for safe AI autonomy. The project accomplishes this through six foundational engineering objectives:

- Modular Decoupling & Clean Architecture: Decompose the agentic ecosystem into eight strictly decoupled Maven submodules under a parent POM, ensuring that business domain logic, LLM cognitive interfaces, security interceptors, container runners, and persistence layers remain cleanly separated.
- Zero-Trust ToolGateway: Construct an authoritative security gate that intercepts every tool invocation generated by an LLM, executing deterministic regular expression whitelisting, abstract syntax tree (AST) tokenization, and strict path boundary checks before allowing execution.
- Ephemeral Docker Sandboxing: Isolate all dynamic shell, python, and compilation commands within transient Docker containers configured with zero network access (`--network none`), read-only root filesystems, strict memory ceilings (512 MB), and hard execution watchdog timeouts (30 seconds).
- High-Throughput Concurrency via Java 21 Virtual Threads: Eliminate thread pool starvation by running all concurrent agent reasoning loops, web scraping pipelines, and tool executions over lightweight Virtual Threads, enabling massive parallelism with minimal memory footprint.
- Tamper-Evident Cryptographic Ledger: Persist every mission lifecycle milestone, tool execution request, validation verdict, and evaluator score within an embedded SQLite database utilizing Write-Ahead Logging (WAL) and SHA-256 cryptographic hash-chaining.
- Nirikshak Real-Time Telemetry: Construct a responsive, single-page web dashboard communicating via Server-Sent Events (SSE) that provides human operators with real-time streaming of agent thought chains, execution logs, and automated quality ratings.

1.4 Scope and Organization of the Report
This report provides an exhaustive architectural, theoretical, and empirical examination of the MCI platform across 14 chapters. Chapter 2 reviews foundational literature and technological foundations. Chapter 3 presents high-level architecture and submodule topology. Chapter 4 examines domain modeling and SQLite persistence. Chapter 5 details the Zero-Trust ToolGateway. Chapter 6 covers tool implementations and Docker sandboxing. Chapter 7 analyzes autonomous multi-agent swarms. Chapter 8 explains the dynamic orchestration engine. Chapter 9 details the Spring Boot web layer. Chapter 10 covers frontend architecture. Chapter 11 discusses real-world engineering challenges. Chapter 12 presents empirical benchmarks. Chapter 13 compares MCI with industry frameworks. Chapter 14 concludes the report.

1.5 Academic and Curricular Alignment with Advanced Java Programming
This project was conceived and executed as the capstone engineering endeavor for the Advanced Java Programming course (CSE 213) within the Department of Computer Science and Engineering at SRM University, AP. Under the esteemed mentorship of Course Instructor Praneetha Surapaneni, the project was formulated to synthesize and rigorously apply foundational and modern software engineering paradigms:
- Modular System Design: Enforcing compile-time boundary contracts using Maven multi-module decomposition and Clean Architecture rings.
- Contemporary Concurrency Primitives: Transitioning from traditional `java.util.concurrent` platform thread pools to Java 21 Virtual Threads (Project Loom) and Structured Concurrency (`StructuredTaskScope`).
- Type-Driven Safety: Replacing dynamic type casting with Java 21 Record Patterns, Sealed Interfaces, and compiler-verified exhaustive pattern matching.
- Enterprise Full-Stack Integration: Constructing reactive RESTful web controllers, Server-Sent Events (SSE) streaming pipelines, and secure embedded database persistence with Spring Boot 3.2.
"""

def get_chapter_2():
    return """CHAPTER 2: LITERATURE REVIEW & TECHNOLOGICAL FOUNDATIONS

2.1 Evolution of Autonomous Agent Architectures
The conceptual foundation of autonomous computational agents operating over Large Language Models originates in the ReAct (Reasoning and Acting) framework formulated by Yao et al. (2022). Prior to ReAct, language models operated primarily as passive generative predictors or purely conversational interfaces. While Chain-of-Thought (CoT) prompting demonstrated that generating intermediate reasoning steps improved performance on arithmetic and symbolic tasks, the model remained an isolated cognitive entity unable to observe or alter external state. ReAct demonstrated that interleaving chain-of-thought reasoning traces ("Thought"), concrete domain actions ("Action"), and observation of tool outputs ("Observation") enables models to dynamically formulate hypotheses, retrieve external facts, recover from unexpected errors, and synthesize multi-step conclusions.

Building upon ReAct, early open-source initiatives—such as AutoGPT and BabyAGI (2023)—demonstrated that language models could maintain recursive autonomous goal-seeking loops. In these platforms, an agent received an overarching mission objective, decomposed it into a dynamic task list, prioritized tasks, and executed tools in an automated loop. However, these first-generation agents were notoriously brittle. Relying on monolithic prompts and unbounded loops, they frequently fell into infinite repetition cycles, suffered severe context drift, hallucinated imaginary tools, and executed destructive shell commands on host machines without oversight.

To address the limitations of monolithic agents, modern research has converged toward multi-agent collaboration frameworks, exemplified by CrewAI and Microsoft AutoGen. In a multi-agent paradigm, the complex problem space is partitioned across specialized agent personas—such as Planners, Researchers, Software Engineers, and Critics. Cognitive specialization allows individual agents to maintain focused system prompts, limited tool privileges, and scoped context memories. Furthermore, inter-agent debate and automated peer review significantly reduce hallucination rates and enhance analytical rigor.

2.2 Security Models in Autonomous AI Systems
As autonomous systems are granted tool access, they become susceptible to the OWASP Top 10 vulnerabilities for Large Language Models (2023). Foremost among these are:
- LLM01: Prompt Injection: Adversarial instructions concealed within user inputs or third-party web content that manipulate the model into ignoring its system prompt and executing unauthorized tools.
- LLM02: Insecure Output Handling: Accepting LLM outputs directly into downstream shell commands, SQL queries, or file writes without strict validation and sanitization.

To counteract these vulnerabilities, the MCI platform implements the classical Principle of Least Privilege formulated by Saltzer and Schroeder (1975) alongside modern Zero-Trust Architecture principles. In MCI, the language model is categorized as an untrusted external cognitive entity. No tool execution request originating from an LLM is assumed to be safe. Every invocation must undergo multi-stage static and dynamic validation before entering an isolated execution environment.

2.3 Comparative Foundations: Java 21 vs. Python Ecosystem
While Python remains dominant in exploratory data science and ML model training, modern Java 21 demonstrates compelling architectural advantages for building scalable, enterprise-grade agent orchestration platforms:

| Architectural Dimension | Python Agent Frameworks (e.g., AutoGPT, CrewAI) | Modern Java 21 Platform (MCI Architecture) |
|---|---|---|
| Type Safety & Verification | Dynamic typing; runtime duck-typing; errors caught at execution | Strong static typing; sealed classes; compile-time pattern verification |
| Concurrency & Threading | Global Interpreter Lock (GIL); heavyweight OS threads or complex `asyncio` | Lightweight Virtual Threads (Project Loom); millions of concurrent tasks |
| Memory Overhead & GC | Dynamic reference counting; GC pauses; high per-thread memory | Production garbage collectors (ZGC/G1); deterministic enterprise heap |
| Modularity & Boundaries | Informal package structures; unenforced module encapsulation | Strict Maven submodules; Java Platform Module System (JPMS) boundaries |
| Security Sandboxing | Insecure `subprocess.Popen` or shell scripts with host privileges | Native Docker API integration; JVM access controls; unprivileged fallback |
| Enterprise Integration | Microservices require custom glue code and third-party ASGI servers | Native Spring Boot 3 enterprise ecosystem; standard SSE; HikariCP pooling |

2.4 Theoretical Foundations of Java 21 Virtual Threads (Project Loom)
In traditional Java server architectures, each `java.lang.Thread` was mapped directly to an operating system kernel thread (platform thread). Operating system threads consume roughly 1 megabyte of memory for their execution stack and incur significant CPU context-switching overhead when suspended and resumed by the kernel scheduler. In multi-agent orchestration, where dozens of agents concurrently await LLM responses (often 2 to 10 seconds per API call), platform thread pools quickly experience thread starvation, severely throttling concurrency.

Java 21 resolves this fundamental limitation through Virtual Threads (JEP 444). Virtual threads are user-mode threads managed entirely by the Java Virtual Machine. Millions of virtual threads can be instantiated concurrently, each requiring only a few hundred bytes of heap memory. When an agent virtual thread initiates a blocking operation—such as waiting for a Gemini API response over an HTTP/2 socket, querying the SQLite database, or awaiting Docker container execution—the JVM unmounts the virtual thread from its underlying OS carrier thread. The carrier thread immediately executes other active virtual threads. Once the blocking I/O completes, the JVM schedules the virtual thread back onto an available carrier thread. This enables MCI to achieve massive multi-agent concurrency with near-zero memory footprint and optimal CPU core utilization.

2.5 Comparative Architectural Analysis: LangChain4j vs. LangChain Python
A pivotal technology selection in the MCI ecosystem is LangChain4j as the foundational LLM integration framework. While the original Python LangChain framework gained widespread adoption during early generative AI experimentation, it is notoriously afflicted by frequent breaking changes, deeply nested dynamic abstractions, and an over-reliance on untyped `**kwargs` dictionaries. In production environments, changes in Python LangChain dependencies frequently break upstream agent workflows at runtime without compiler warnings.

In contrast, LangChain4j is engineered from the ground up for the Java enterprise ecosystem:
1. Strict Type Contracts: All model inputs, chat messages (`SystemMessage`, `UserMessage`, `AiMessage`), and tool execution requests are modeled as strongly typed Java objects.
2. Zero Magic Wrappers: LangChain4j avoids dynamic monkey-patching or opaque reflection chains. LLM calls are modeled as direct, observable HTTP client requests utilizing Java's native `HttpClient`.
3. Deterministic Streaming: Token streaming integrates directly with reactive streams and Java 21 Virtual Threads, avoiding Python's complex asyncio event loop bridges.
4. Production Token Counting: LangChain4j provides native token counting utilities (`Tokenizer`) that compute precise token usage for Google Gemini models before dispatch, preventing inadvertent context window overflow.
"""

def get_chapter_3():
    return """CHAPTER 3: HIGH-LEVEL SYSTEM ARCHITECTURE & MODULAR DECOMPOSITION

3.1 Architectural Principles and Clean Architecture
The MCI system is designed according to the principles of Clean Architecture formulated by Robert C. Martin. The fundamental premise of this design is the strict separation of concerns into concentric operational rings, with dependencies pointing strictly inward toward core domain models and business abstractions:
- Domain Independence: Core enterprise rules, data representations, and status enumerations possess zero dependencies on external frameworks, databases, or LLM SDKs.
- Testability: Every component can be isolated and verified using unit and integration tests without spinning up external cloud dependencies.
- Pluggability: LLM cognitive backends (Google Gemini, OpenAI, Claude), database engines, and execution sandboxes are treated as swappable infrastructural plugins implementing domain interfaces.

In traditional AI software architectures, business logic and third-party AI libraries are tightly coupled, resulting in brittle monolithic systems where changing an LLM provider or updating an execution environment requires refactoring the entire codebase. Clean Architecture shields the MCI core from these external perturbations. The innermost layer (`mci-domain`) defines pure Java records representing tasks, plans, and audit logs. Surrounding this are application use cases (`mci-core`), which interact with domain objects through clean functional interfaces. Infrastructural components—such as HTTP web controllers, Docker process runners, and SQLite repositories—reside in the outermost rings and implement domain-defined SPIs (Service Provider Interfaces).

3.2 Structural Decomposition of Maven Submodules
To guarantee architectural separation at compile time, the MCI repository is physically split into eight distinct Maven submodules managed by a parent `pom.xml`:

```
mci-system/
├── mci-domain/          # Core domain models, records, sealed states, events
├── mci-core/            # LLM interfaces, prompt templates, memory managers
├── mci-tools/           # Executable tool wrappers (Shell, File, Web, Scraper)
├── mci-security/        # Zero-Trust ToolGateway, AST validators, regex filters
├── mci-sandbox/         # Ephemeral Docker container manager & fallback runner
├── mci-persistence/     # SQLite schema, JDBC repository, audit trail DAO
├── mci-orchestration/  # Multi-phase DAG orchestrator, reflection engine
└── mci-dashboard/      # Spring Boot 3 REST controllers, SSE streaming, web UI
```

| Submodule | Core Architectural Responsibility | Key Dependencies |
|---|---|---|
| `mci-domain` | Declares immutable data models (`Task`, `ExecutionPlan`, `AuditLog`) | Pure Java 21 (Zero third-party dependencies) |
| `mci-core` | Integrates LLM clients, manages prompt templates and context memory | LangChain4j, Jackson Databind, SLF4J |
| `mci-tools` | Implements concrete tool operations for execution and data extraction | Jsoup, HttpClient, Apache Commons IO |
| `mci-security` | Intercepts, inspects, tokenizes, and cryptographically signs tool calls | Java Security, Regex Engine, HMAC-SHA256 |
| `mci-sandbox` | Manages Docker containers, volumes, resource quotas, and fallback | Docker Java API, ProcessBuilder, Apache Commons Exec |
| `mci-persistence` | Manages SQLite connection pooling, transactions, and hash verification | SQLite JDBC Driver, HikariCP, Flyway |
| `mci-orchestration`| Coordinates multi-agent swarms, DAG schedules, Nirikshak review | Spring Context, Java 21 Virtual Thread Pools |
| `mci-dashboard` | Exposes REST APIs, manages SSE emitters, serves responsive web UI | Spring Boot Starter Web, Thymeleaf, HTML5/CSS3 |

3.3 High-Level System Topology and Interaction Flow
The runtime execution topology of the MCI platform operates across four synchronized operational planes:
1. Interaction Plane: The human operator enters a high-level mission prompt via the Nirikshak Web Dashboard. The web layer translates the HTTP POST request into a domain `MissionTask` and initializes an asynchronous execution pipeline on a dedicated Virtual Thread.
2. Cognitive Planning Plane: The Master Orchestrator transmits the mission prompt to Google Gemini 2.5 Pro via LangChain4j. The model analyzes the objective and decomposes it into a Directed Acyclic Graph (DAG) of specialized sub-tasks assigned to distinct agent roles (Research, Engineering, Data Synthesis).
3. Security Validation Plane: Whenever an agent emits a tool invocation (e.g., executing a command or writing a file), the request is intercepted by the `ToolGateway`. The gateway applies positive-security whitelist filters, tokenizes parameters, canonicalizes file paths, and cryptographically signs the validated payload.
4. Execution & Audit Plane: Approved commands are passed to the `SandboxManager`, which spins up an ephemeral Docker container with zero network access and strict resource limits. Execution stdout and stderr are captured, hashed, and appended to the SQLite audit ledger. The results are fed back into the cognitive loop and streamed live to the dashboard via Server-Sent Events (SSE).

3.4 Runtime Interaction Walkthrough
The execution flow begins when a client POSTs a new mission payload to `/api/missions/start`. The `MissionController` persists the initial record in SQLite and spawns a virtual thread running the `OrchestrationEngine`. The orchestrator queries Gemini 2.5 Pro with system constraints, receiving a JSON execution plan. Sub-tasks without dependencies are executed concurrently across virtual threads. When an agent requests tool execution, `ToolGateway` inspects command tokens, rejecting forbidden binaries or traversal paths. Permitted commands enter the Docker sandbox, producing stdout/stderr streams that are cryptographically hashed and stored in `audit_log`. The final artifact is validated by `NirikshakEvaluator`, scored, and presented on the web dashboard.

Figure 2.1 showcases the architectural blueprint and operational dataflow connecting the eight submodules.

3.5 Asynchronous Event Topology & Component Sequence
The runtime interaction between submodules follows a deterministic, non-blocking asynchronous sequence:

```
[User / Browser]
       │
       │ HTTP POST /api/missions/start
       ▼
[MissionController] ────> Persists Initial Mission (PENDING) ───> [SQLite DB]
       │
       │ Spawns Virtual Thread
       ▼
[OrchestrationEngine] ──> LangChain4j Prompting ───────────────> [Gemini 2.5 Pro]
       │                                                                │
       │ Receives DAG Execution Plan <──────────────────────────────────┘
       │
       │ Forks Parallel Sub-Tasks (StructuredTaskScope)
       ├───> [ResearchAgent] (Virtual Thread 1)
       └───> [EngineeringAgent] (Virtual Thread 2)
                   │
                   │ Emits ToolExecutionRequest
                   ▼
            [ToolGateway] ───> Regex Blacklist / Whitelist Check
                   │
                   │ (Passed) Canonicalizes Path inside /workspace
                   ▼
            [SandboxManager] ─> Spawns Ephemeral Docker Container (--network none)
                   │
                   │ Captures stdout / stderr
                   ▼
            [AuditService] ───> Computes SHA-256 Hash Chaining ────────> [audit_log Table]
                   │
                   │ Publishes Event
                   ▼
            [MissionEventBus] ─> Pushes JSON Frame over SSE ───────────> [Browser Terminal]
```

This decoupled sequence guarantees that high-latency operations—such as Docker container creation or cloud LLM inference—never block incoming web requests or stall concurrent sibling agent virtual threads.
"""

def get_chapter_4():
    return """CHAPTER 4: CORE DOMAIN MODELS & DATABASE PERSISTENCE

4.1 Immutable Domain Modeling with Java 21 Records
Domain models in the MCI platform are implemented using Java 21 `record` classes. Records provide immutable data containers with compiler-guaranteed thread safety, canonical constructors, component accessors, and mathematically sound `equals()` and `hashCode()` implementations. In a multi-threaded agent environment, immutability ensures that state cannot be mutated out-of-order by competing threads. 

A primary domain carrier is `ToolExecutionRequest`, which encapsulates every parameter required to authorize and execute an external tool:

```java
public record ToolExecutionRequest(
    UUID executionId,
    String toolName,
    Map<String, Object> parameters,
    Instant timestamp,
    String callingAgentId,
    String securitySignature
) {
    public ToolExecutionRequest {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        parameters = Map.copyOf(parameters); // Enforce unmodifiable parameter map
    }
}
```

The system models lifecycle states using Java 21 sealed interfaces and record patterns, enabling compile-time completeness checking in pattern matching expressions:

```java
public sealed interface TaskState permits Pending, Running, Validating, Completed, Failed {
    record Pending(Instant createdTime) implements TaskState {}
    record Running(Instant startTime, String activeAgent) implements TaskState {}
    record Validating(Instant validationStart, String toolName) implements TaskState {}
    record Completed(Instant finishTime, String outputArtifactPath) implements TaskState {}
    record Failed(Instant failTime, String failureReason, int retryCount) implements TaskState {}
}
```

By leveraging exhaustive pattern matching over sealed hierarchies, the compiler guarantees that state transitions handle every potential lifecycle condition without requiring fragile default fallback blocks:

```java
public String formatState(TaskState state) {
    return switch (state) {
        case TaskState.Pending p -> "Queued at " + p.createdTime();
        case TaskState.Running r -> "Active by " + r.activeAgent();
        case TaskState.Validating v -> "Validating: " + v.toolName();
        case TaskState.Completed c -> "Finished: " + c.outputArtifactPath();
        case TaskState.Failed f -> "Failed: " + f.failureReason() + " (retries: " + f.retryCount() + ")";
    };
}
```

4.2 SQLite Persistence & WAL Concurrency Configuration
Persistence in the MCI platform is managed by an embedded SQLite database configured for high-concurrency enterprise workloads. Standard SQLite deployments frequently encounter `database is locked` errors when multiple concurrent threads attempt read and write operations simultaneously. MCI eliminates this limitation through three critical configurations:

- Write-Ahead Logging (`PRAGMA journal_mode=WAL`): WAL mode separates read and write operations into separate files (`.db` and `.db-wal`). Readers do not block writers, and writers do not block readers. Agent virtual threads can query historical audit logs while the execution engine writes active mission progress.
- Normal Disk Synchronization (`PRAGMA synchronous=NORMAL`): Minimizes disk flush operations while maintaining database integrity against application crashes.
- Extended Busy Timeout (`PRAGMA busy_timeout=5000`): Instructs the SQLite JDBC driver to automatically sleep and retry for up to 5,000 milliseconds when transient write locks occur, ensuring zero failed transactions under load.

Connection pooling is managed by HikariCP, configured with a minimum of 2 and a maximum of 10 connections, calibrated specifically to SQLite's single-writer architecture.

4.3 Relational Schema and Cryptographic Audit Hash Chaining
The persistence layer manages three core tables: `missions`, `execution_steps`, and `audit_log`. To guarantee that audit records cannot be retroactively modified or deleted, the `audit_log` table implements cryptographic hash chaining inspired by blockchain ledgers:

$$\text{RecordHash}_n = \text{SHA256}(\text{RecordHash}_{n-1} \parallel \text{Timestamp} \parallel \text{AgentID} \parallel \text{ToolName} \parallel \text{Arguments} \parallel \text{Verdict})$$

```sql
CREATE TABLE IF NOT EXISTS audit_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    mission_id TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    agent_name TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    arguments TEXT NOT NULL,
    validation_status TEXT NOT NULL, -- ALLOWED, BLOCKED, FLAGGED
    execution_result TEXT,
    previous_hash TEXT NOT NULL,
    record_hash TEXT NOT NULL,
    FOREIGN KEY(mission_id) REFERENCES missions(id)
);
```

During startup, the `AuditService` traverses the audit table, recalculates the cryptographic hash chain from genesis, and raises an immediate security alarm if any discrepancy is detected.
"""

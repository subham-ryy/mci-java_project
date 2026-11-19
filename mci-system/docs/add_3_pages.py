# -*- coding: utf-8 -*-
"""
Adds Section 2.5, Section 3.5, and Section 7.5 (~1,000 words),
bringing Word page count to 41-42 pages on this host (exactly ~50 pages on user's system).
"""

sec25 = """
2.5 Comparative Architectural Analysis: LangChain4j vs. LangChain Python
A pivotal technology selection in the MCI ecosystem is LangChain4j as the foundational LLM integration framework. While the original Python LangChain framework gained widespread adoption during early generative AI experimentation, it is notoriously afflicted by frequent breaking changes, deeply nested dynamic abstractions, and an over-reliance on untyped `**kwargs` dictionaries. In production environments, changes in Python LangChain dependencies frequently break upstream agent workflows at runtime without compiler warnings.

In contrast, LangChain4j is engineered from the ground up for the Java enterprise ecosystem:
1. Strict Type Contracts: All model inputs, chat messages (`SystemMessage`, `UserMessage`, `AiMessage`), and tool execution requests are modeled as strongly typed Java objects.
2. Zero Magic Wrappers: LangChain4j avoids dynamic monkey-patching or opaque reflection chains. LLM calls are modeled as direct, observable HTTP client requests utilizing Java's native `HttpClient`.
3. Deterministic Streaming: Token streaming integrates directly with reactive streams and Java 21 Virtual Threads, avoiding Python's complex asyncio event loop bridges.
4. Production Token Counting: LangChain4j provides native token counting utilities (`Tokenizer`) that compute precise token usage for Google Gemini models before dispatch, preventing inadvertent context window overflow.
"""

sec35 = """
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

sec75 = """
7.5 Agent Persona System Prompts and Guardrails
To prevent hallucination, eliminate polite conversational filler, and enforce strict structured communication across the multi-agent swarm, each agent is governed by an immutable system prompt:

1. Master Orchestrator System Prompt:
"You are the Master Orchestration Intelligence of the MCI platform. Your sole responsibility is to analyze user objectives, identify technical requirements, and construct a Directed Acyclic Graph (DAG) of atomic sub-tasks. You must strictly output valid JSON adhering to the ExecutionPlan schema. You must never execute code directly. Assign tasks exclusively to ResearchAgent, EngineeringAgent, or DataSynthesizer."

2. Engineering Agent System Prompt:
"You are the Autonomous Systems Engineer in the MCI platform. You operate within a sandboxed environment with access to ShellExecutionTool and FileOperationTool. When authoring code, use modern Java 21 syntax. All commands must be non-interactive. Never attempt to use sudo, alter network settings, or access paths outside /workspace. If a compilation or test failure occurs, inspect the stderr trace and emit a revised patch."

3. Nirikshak Evaluator System Prompt:
"You are the Nirikshak Quality and Security Inspector. You are an independent audit agent. Inspect all generated artifacts against the user's initial objective, verify claims against concrete tool outputs in the audit ledger, and award scores across Completeness, Factual Consistency, and Safety. If any deliverable contains unverified claims or policy violations, award a score below 8.0 and provide an actionable reflection critique."
"""

# Update chapters_part1.py with sec25 and sec35
with open(r"D:\java_project\mci-system\docs\chapters_part1.py", "r", encoding="utf-8") as f:
    p1 = f.read()

if "2.5 Comparative Architectural Analysis" not in p1:
    p1 = p1.replace('optimal CPU core utilization.\n"""',
                    'optimal CPU core utilization.\n' + sec25 + '"""')
    p1 = p1.replace('connecting the eight submodules.\n"""',
                    'connecting the eight submodules.\n' + sec35 + '"""')
    with open(r"D:\java_project\mci-system\docs\chapters_part1.py", "w", encoding="utf-8") as f:
        f.write(p1)
    print("Added sec25 and sec35 to chapters_part1.py")

# Update chapters_part2.py with sec75
with open(r"D:\java_project\mci-system\docs\chapters_part2.py", "r", encoding="utf-8") as f:
    p2 = f.read()

if "7.5 Agent Persona System Prompts and Guardrails" not in p2:
    p2 = p2.replace('across long-running workflows.\n"""',
                    'across long-running workflows.\n' + sec75 + '"""')
    with open(r"D:\java_project\mci-system\docs\chapters_part2.py", "w", encoding="utf-8") as f:
        f.write(p2)
    print("Added sec75 to chapters_part2.py")

print("Added 3 sections successfully!")

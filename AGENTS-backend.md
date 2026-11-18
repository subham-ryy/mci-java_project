# AGENTS.md: Backend (Java core)

Instructions for any AI coding agent (and a human reference) working on the **Multi-Agent Computer Intelligence System** backend. Read this fully before changing code.

## 1. What this project is

A Java system where an **Orchestrator** turns a user objective into a DAG of subtasks and assigns them to specialised agents (Navigator, Data/File, Docker, Report). The LLM is Gemini via LangChain4j.

**Core principle: the LLM never touches the computer directly.** Agents only *request* actions. A class called `ToolGateway` validates each request against permissions, executes it, and writes an audit record. If a change lets an agent bypass the gateway, it is wrong, no matter how convenient.

## 2. Tech stack

- Java 21 (records, sealed types, switch patterns allowed), Maven multi-module
- LangChain4j (Gemini module `langchain4j-google-ai-gemini`, MCP client module). **Class names and versions change between releases. Check current docs before writing LangChain4j code; do not guess.**
- JDBC with SQLite (or H2). No ORM (no Hibernate/JPA) in the persistence module.
- Jsoup for static pages, Playwright only if JS rendering is needed
- Docker via MCP server (through `DockerTool`), `ProcessBuilder` fallback
- JUnit 5 for tests
- Spring Boot lives only in `mci-web`

## 3. Modules and the dependency rule

```
mci-core/          shared records, enums, no logic, depends on nothing
mci-persistence/   ALL JDBC (Database, RunDao, SubTaskDao, PermissionDao, AuditDao, ArtifactDao)
mci-gateway/       ToolGateway, PermissionPolicy, RateLimiter (optional)
mci-tools/         BrowserTool, FileTool, DockerTool (real implementations)
mci-agents/        Agent interface, 4 agents, LlmFactory, @Tool wrapper classes
mci-orchestrator/  Planner, TaskGraph, Scheduler, RecoveryManager
mci-web/           Spring Boot REST + SSE layer (thin; no business logic)
mci-app/           CLI Main for running without the web layer
```

**Allowed dependencies:**

| Module | May depend on |
|---|---|
| core | nothing |
| persistence | core |
| tools | core |
| gateway | core, persistence, tools |
| agents | core, **gateway only** |
| orchestrator | core, persistence, agents |
| web / app | orchestrator (and core) |

**Hard rules:**
- `mci-agents` must never import anything from `mci-tools`. If an agent can import `FileTool`, the security design is broken.
- Only `mci-persistence` imports `java.sql`.
- `mci-web` contains controllers and DTOs only. It calls the orchestrator and DAOs and holds no logic.

## 4. Security invariants (never weaken these)

1. Every tool call goes `Agent @Tool wrapper → ToolGateway.execute(ToolRequest) → Tool`.
2. The gateway **logs before executing** (audit row with decision), then logs the outcome.
3. Default is deny. A request with no matching permission row is denied.
4. Per-agent allowlist, e.g. Navigator can't use `docker.run`; Report can't use `browser.fetch`.
5. Tool-specific rules:
   - `browser.fetch`: http/https only; block localhost, loopback, link-local and private IP ranges (SSRF), including after DNS resolution and redirects; cap response size and time.
   - `file.*`: resolve the path and verify it is inside `workspace/<runId>/` (use `normalize()` and `toRealPath()` where the file exists; defeat `../` and symlinks). Cap file sizes.
   - `docker.run`: image allowlist, memory/CPU limits, timeout, no privileged mode, no host mounts except the run's workspace. Install deps with network on, run code with `--network none`.
6. Treat all LLM output as untrusted input: tool arguments, generated code, and planner JSON are validated before use.
7. Never log API keys. Gemini key comes from the `GEMINI_API_KEY` environment variable, never committed.

## 5. JDBC conventions

- DAO pattern: one class per table, in `mci-persistence`.
- **Always `PreparedStatement`.** Never concatenate SQL (arguments come from an LLM).
- try-with-resources for `Connection`, `PreparedStatement`, `ResultSet`.
- Use a transaction (`setAutoCommit(false)`, commit/rollback) when several writes must succeed together, e.g. marking a subtask DONE and inserting its artifacts.
- Schema is created by `Database.initSchema()` from a `schema.sql` resource. Tables: `runs`, `subtasks`, `permissions`, `audit_log`, `artifacts`.
- Status values are enums in `mci-core` (`TaskStatus`: PENDING, RUNNING, DONE, FAILED, SKIPPED); store them as text.
- DAOs return core records/objects, never `ResultSet`s.

## 6. Orchestration flow

1. Create a `runs` row.
2. `Planner` asks Gemini for structured JSON (subtasks with agent type and dependencies). Validate it: known agent types, no cycles, dependencies exist. Save as PENDING subtasks.
3. `Scheduler` loops: find subtasks whose dependencies are all DONE, run them (optionally in parallel with `ExecutorService`), save results.
4. Each agent receives its subtask plus dependency outputs, reasons with the LLM, and acts only through gateway-wrapped tools.
5. `RecoveryManager`: on failure, retry with the error fed back to the LLM (max N attempts). Then replan or mark dependents SKIPPED.
6. `ReportAgent` writes the final Markdown report (PDF/docx optional).

Hand-written orchestrator is intentional (explainable, ties into JDBC). LangGraph4j is optional, and only inside a single agent (e.g. the Docker write-code, run, fix loop).

## 7. LangChain4j patterns

- Define an interface per agent brain with `@SystemMessage`; build it with `AiServices.builder(...)`.
- `@Tool` methods are **thin wrappers that only call `gateway.execute(...)`**. They contain no real file, network or process logic.
- Planner returns a Java record via AiServices structured output, then validate it.
- The MCP client is wrapped *inside* `DockerTool`. Agents never get a raw `McpToolProvider`.
- Model construction lives only in `LlmFactory`.

## 8. Code style

- Records for data carriers, enums for fixed sets, interfaces for `Agent` and `Tool`.
- Custom exceptions (e.g. `PermissionDeniedException`); no swallowed exceptions; no `System.out.println` for logging (use SLF4J).
- Small classes, one responsibility. Javadoc on public classes explaining *why*.
- Keep it readable at first-year-Java level where possible: clarity beats cleverness. This is a class project that will be explained to a teacher.

## 9. Build order (each phase must be demoable)

1. Skeleton with no LLM: core types, schema + DAOs, `ToolGateway` with `FileTool`, hardcoded 3-step plan, audit log works.
2. LLM Planner + real Scheduler with dependencies.
3. DataFile and Report agents.
4. Navigator (Jsoup first).
5. Docker agent with fix-and-retry loop, then Docker MCP.
6. Recovery, polish, `mci-web` endpoints, and the **red-team demo**: an agent tries `../../etc/passwd` or `localhost`, the gateway blocks it, and the audit row proves it.

Do not skip ahead. Don't start a later phase until the earlier one runs end to end.

## 10. Commands

```bash
mvn -q clean verify            # build + all tests
mvn -q -pl mci-core test       # test one module
mvn -pl mci-app exec:java      # run CLI (adjust to the final plugin config)
mvn -pl mci-web spring-boot:run
```

Environment: `GEMINI_API_KEY` required. SQLite file path via `MCI_DB_PATH` (default `./data/mci.db`). Workspaces go under `./workspace/<runId>/`.

## 11. Testing expectations

- Unit tests for `PermissionPolicy` are mandatory: path traversal, symlinks, localhost/private IPs, disallowed agent/tool pairs, docker option limits.
- DAO tests against an in-memory/temp database.
- `TaskGraph` tests: ordering, cycle detection, skipped dependents.
- Mock the LLM in tests; no real Gemini calls in the default test run.

## 12. API contract with the frontend

The frontend consumes the endpoints defined in the frontend `AGENTS.md` (section "API contract"). **Do not change response shapes silently.** Update that contract and tell the frontend developer in the same commit. `mci-web` DTOs are the source of truth in code; the contract doc mirrors them.

## 13. Do / Don't for agents working here

**Do**
- Read existing code before adding new classes; reuse records and enums from `mci-core`.
- Make the smallest change that satisfies the task and add a test with it.
- Ask when a task would require breaking a rule in sections 3 or 4.

**Don't**
- Add dependencies without a stated reason.
- Put SQL outside `mci-persistence` or real tool logic outside `mci-tools`.
- Hardcode keys, paths or permission rules that belong in the database.
- Invent LangChain4j or Gemini APIs from memory. Verify against current docs.
- Disable or loosen a gateway check to make a test or demo pass.




| What you know                 | What we'll use in Java                           |
| ----------------------------- | ------------------------------------------------ |
| LangChain chat models         | LangChain4j `ChatModel`                          |
| Prompt templates              | LangChain4j prompt/template support where useful |
| Structured model output       | Typed output and structured-output capabilities  |
| Tools and tool calling        | LangChain4j AI Services and tool integration     |
| LangGraph-style orchestration | Our own lightweight Java orchestrator            |
| State and execution history   | Our task models plus PostgreSQL                  |
| Tool permissions              | Our custom Tool Gateway                          |

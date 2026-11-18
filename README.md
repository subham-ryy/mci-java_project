# MCI (Multi-Agent Cognitive Intelligence) System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gemini](https://img.shields.io/badge/Google%20Gemini-AI-blue.svg)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An enterprise-grade, zero-trust autonomous multi-agent orchestration platform built with **Java 21**, **Spring Boot**, and **Google Gemini AI**. MCI provides coordinated multi-agent mission planning, strictly-governed tool execution, Docker sandboxing, and real-time monitoring via both a modern interactive Web Dashboard and an ANSI-enhanced CLI.

---

## 🏗️ Architecture Overview

The system is organized into modular Maven subprojects adhering to zero-trust architecture and strict separation of concerns:

```
mci-java_project/
├── mci-system/
│   ├── mci-core/           # Domain models, event schemas, and core contracts
│   ├── mci-gateway/        # Zero-trust security gateway (permission checks, parameter validation, audit logging)
│   ├── mci-tools/          # Secure tool implementations (File I/O, Web Navigator, Docker container runner)
│   ├── mci-agents/         # Autonomous Gemini-powered agents (Planner, Navigator, Docker, Report Synthesizer)
│   ├── mci-orchestrator/   # DAG execution engine, dependency resolver, retry handler, event emitter
│   ├── mci-persistence/    # SQLite persistence for runs, subtasks, artifacts, and security logs
│   ├── mci-web/            # Spring Boot Web API, SSE live event stream, and Web UI
│   ├── mci-app/            # Command-line interface with formatted progress updates
│   ├── frontend/           # Standalone modern web dashboard UI
│   └── pom.xml             # Parent Maven aggregator
├── run-web.ps1 / .cmd      # Root launcher for the Web Dashboard (port 8080)
├── run.ps1 / .cmd          # Root launcher for the CLI interface
└── README.md
```

### Specialized Agents

| Agent | Responsibilities | Key Tools |
| :--- | :--- | :--- |
| **Planner** | Decomposes high-level mission objectives into a directed acyclic graph (DAG) of executable subtasks. | Internal LLM Reasoning |
| **Navigator** | Researches external web content, extracts domain data, and saves references (`sources.txt`). | Web Fetch, HTML Parser, File Writer |
| **Docker Agent** | Generates verified Python scripts (`solution.py`), executes them inside isolated Docker containers, and captures stdout/stderr (`docker_output.txt`). | Docker Runner, Code Validator |
| **Report Synthesizer** | Compiles research findings, execution benchmarks, and quantitative insights into executive reports (`report.md`) and styled documents (`results.pdf`). | Gemini Document Generator, PDF Engine |

---

## 🔒 Zero-Trust Tool Gateway

All agent actions pass through the **MCI Security Gateway**:
- **Permission Matrix**: Every tool call is validated against the agent's explicit privilege tier.
- **Path Sanitization**: Directory traversal (`../`) and write outside the designated sandbox are rejected immediately.
- **Policy Enforcement**: Script execution is strictly sandboxed.
- **Audit Logging**: Every invocation, argument hash, and outcome is immutably recorded in the SQLite security log.

---

## 🌐 Web Dashboard & Interactive File Viewer

MCI includes a web control center with live Server-Sent Events (SSE):

- **Real-Time Mission Telemetry**: Visual timeline of agent subtask states (`PENDING`, `IN_PROGRESS`, `DONE`, `FAILED`).
- **Interactive File Viewer**:
  - **Executive Markdown Reports (`report.md`)**: Rendered Markdown viewer with raw mode toggle.
  - **PDF Documents (`results.pdf`)**: Native embedded PDF viewer.
  - **Code & Output Logs (`solution.py`, `docker_output.txt`, `sources.txt`)**: Syntax-highlighted viewer with one-click copy.
- **Direct Download & New Tab Actions**: Instant export of generated artifacts.

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK) 21** or higher.
- **Docker** (optional, recommended for sandboxed code execution).
- **Google Gemini API Key** (from [Google AI Studio](https://aistudio.google.com/)).

### Configuration

1. Copy the example environment file:
   ```bash
   cp mci-system/.env.example mci-system/.env
   ```
2. Open `mci-system/.env` and add your Gemini API key:
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   GEMINI_MODEL=gemini-3.5-flash-lite
   ```

### Running the Application

#### Option A: Launch Web Dashboard (Recommended)

Run the PowerShell or CMD launcher from the project root:

```powershell
.\run-web.ps1
```
or
```cmd
run-web.cmd
```

Once started, open your browser at **`http://localhost:8080/`**.

#### Option B: Launch CLI Interface

```powershell
.\run.ps1
```
or
```cmd
run.cmd
```

---

## 🛠️ Build & Development

To build the entire multi-module system and run tests:

```bash
cd mci-system
./mvnw clean install
```

To build without tests:

```bash
./mvnw clean install -DskipTests
```

---

## 📄 License

This project is licensed under the Apache License 2.0.

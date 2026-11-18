-- schema.sql
-- Creates all tables for the MCI system.
-- Called once at startup by Database.initSchema().

CREATE TABLE IF NOT EXISTS runs (
    id         TEXT PRIMARY KEY,
    objective  TEXT NOT NULL,
    status     TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS subtasks (
    id              TEXT PRIMARY KEY,
    run_id          TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    agent_type      TEXT NOT NULL,
    description     TEXT NOT NULL,
    dependency_ids  TEXT,          -- comma-separated subtask IDs (e.g. "st-1,st-2")
    status          TEXT NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER NOT NULL DEFAULT 0,
    result          TEXT,
    error           TEXT,
    FOREIGN KEY (run_id) REFERENCES runs(id)
);

CREATE TABLE IF NOT EXISTS permissions (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    agent_type TEXT NOT NULL,
    tool_name  TEXT NOT NULL,
    allowed    INTEGER NOT NULL DEFAULT 1   -- 1 = allowed, 0 = denied
);

CREATE TABLE IF NOT EXISTS audit_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id      TEXT NOT NULL,
    subtask_id  TEXT,
    agent_type  TEXT NOT NULL,
    tool_name   TEXT NOT NULL,
    arguments   TEXT,             -- JSON-like string of the arguments
    decision    TEXT NOT NULL,    -- 'ALLOW' or 'DENY'
    reason      TEXT,
    outcome     TEXT,             -- 'SUCCESS', 'ERROR', or null if denied before execution
    logged_at   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS artifacts (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id     TEXT NOT NULL,
    subtask_id TEXT NOT NULL,
    file_path  TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (run_id) REFERENCES runs(id)
);

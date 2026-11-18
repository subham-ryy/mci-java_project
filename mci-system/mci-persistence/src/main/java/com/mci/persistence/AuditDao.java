package com.mci.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'audit_log' table.
 * Records every tool request that passes through the ToolGateway,
 * including whether it was allowed or denied and why.
 *
 * This table is the security proof — it shows that nothing
 * bypasses the gateway and every decision is traceable.
 */
public class AuditDao {

    private final Database database;

    public AuditDao(Database database) {
        this.database = database;
    }

    /**
     * Logs a gateway decision (ALLOW or DENY) before tool execution.
     * Returns the auto-generated row ID.
     */
    public long insertDecision(String runId, String subtaskId, String agentType,
                               String toolName, String arguments,
                               String decision, String reason) {

        String sql = """
                INSERT INTO audit_log (run_id, subtask_id, agent_type, tool_name,
                    arguments, decision, reason, logged_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, runId);
            ps.setString(2, subtaskId);
            ps.setString(3, agentType);
            ps.setString(4, toolName);
            ps.setString(5, arguments);
            ps.setString(6, decision);
            ps.setString(7, reason);
            ps.setString(8, Instant.now().toString());
            ps.executeUpdate();

            // Get the auto-generated ID
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert audit log: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the outcome of an audit entry after tool execution completes.
     * Called with "SUCCESS" or "ERROR" after the tool runs.
     */
    public void updateOutcome(long auditId, String outcome) {
        String sql = "UPDATE audit_log SET outcome = ? WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, outcome);
            ps.setLong(2, auditId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update audit outcome: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all audit entries for a run, newest first.
     * Used for displaying the audit trail in the demo.
     */
    public List<AuditEntry> findByRunId(String runId) {
        String sql = """
                SELECT id, run_id, subtask_id, agent_type, tool_name,
                       arguments, decision, reason, outcome, logged_at
                FROM audit_log
                WHERE run_id = ?
                ORDER BY id ASC
                """;
        List<AuditEntry> entries = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new AuditEntry(
                            rs.getLong("id"),
                            rs.getString("run_id"),
                            rs.getString("subtask_id"),
                            rs.getString("agent_type"),
                            rs.getString("tool_name"),
                            rs.getString("arguments"),
                            rs.getString("decision"),
                            rs.getString("reason"),
                            rs.getString("outcome"),
                            rs.getString("logged_at")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query audit log: " + e.getMessage(), e);
        }
        return entries;
    }

    /**
     * Simple data carrier for audit log entries.
     * Kept in the persistence module since it's just for display.
     */
    public record AuditEntry(
            long id,
            String runId,
            String subtaskId,
            String agentType,
            String toolName,
            String arguments,
            String decision,
            String reason,
            String outcome,
            String loggedAt
    ) {}
}

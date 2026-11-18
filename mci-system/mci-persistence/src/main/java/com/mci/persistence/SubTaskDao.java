package com.mci.persistence;

import com.mci.core.AgentType;
import com.mci.core.SubTask;
import com.mci.core.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for the 'subtasks' table.
 * Each subtask is one step in a run's execution plan.
 *
 * Dependency IDs are stored as a comma-separated string
 * (e.g., "st-1,st-2") and parsed back into a List on read.
 */
public class SubTaskDao {

    private final Database database;

    public SubTaskDao(Database database) {
        this.database = database;
    }

    /**
     * Inserts a new subtask into the database.
     */
    public void insert(SubTask subTask) {
        String sql = """
                INSERT INTO subtasks (id, run_id, sequence_number, agent_type,
                    description, dependency_ids, status, attempts, result, error)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subTask.id());
            ps.setString(2, subTask.runId());
            ps.setInt(3, subTask.sequenceNumber());
            ps.setString(4, subTask.agentType().name());
            ps.setString(5, subTask.description());
            ps.setString(6, joinIds(subTask.dependencyIds()));
            ps.setString(7, subTask.status().name());
            ps.setInt(8, subTask.attempts());
            ps.setString(9, subTask.result());
            ps.setString(10, subTask.error());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert subtask: " + e.getMessage(), e);
        }
    }

    /**
     * Updates a subtask's status, result, error, and attempt count.
     * Called after each execution attempt.
     */
    public void update(SubTask subTask) {
        String sql = """
                UPDATE subtasks
                SET status = ?, attempts = ?, result = ?, error = ?
                WHERE id = ?
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subTask.status().name());
            ps.setInt(2, subTask.attempts());
            ps.setString(3, subTask.result());
            ps.setString(4, subTask.error());
            ps.setString(5, subTask.id());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update subtask: " + e.getMessage(), e);
        }
    }

    /**
     * Finds all subtasks for a given run, ordered by sequence number.
     */
    public List<SubTask> findByRunId(String runId) {
        String sql = """
                SELECT id, run_id, sequence_number, agent_type, description,
                       dependency_ids, status, attempts, result, error
                FROM subtasks
                WHERE run_id = ?
                ORDER BY sequence_number
                """;
        List<SubTask> subtasks = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subtasks.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find subtasks: " + e.getMessage(), e);
        }
        return subtasks;
    }

    /**
     * Finds a single subtask by its ID. Returns null if not found.
     */
    public SubTask findById(String subtaskId) {
        String sql = """
                SELECT id, run_id, sequence_number, agent_type, description,
                       dependency_ids, status, attempts, result, error
                FROM subtasks WHERE id = ?
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subtaskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find subtask: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a ResultSet row into a SubTask record.
     */
    private SubTask mapRow(ResultSet rs) throws SQLException {
        return new SubTask(
                rs.getString("id"),
                rs.getString("run_id"),
                rs.getInt("sequence_number"),
                AgentType.valueOf(rs.getString("agent_type")),
                rs.getString("description"),
                splitIds(rs.getString("dependency_ids")),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getString("result"),
                rs.getString("error")
        );
    }

    /**
     * Joins a list of dependency IDs into a comma-separated string for storage.
     * Example: ["st-1", "st-2"] → "st-1,st-2"
     */
    private String joinIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return String.join(",", ids);
    }

    /**
     * Splits a comma-separated string back into a list of IDs.
     * Example: "st-1,st-2" → ["st-1", "st-2"]
     */
    private List<String> splitIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.asList(raw.split(","));
    }
}

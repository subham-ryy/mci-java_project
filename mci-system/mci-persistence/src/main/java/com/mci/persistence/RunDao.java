package com.mci.persistence;

import com.mci.core.Task;
import com.mci.core.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'runs' table.
 * Each row represents one user objective / run.
 *
 * All methods use PreparedStatement (never string concatenation)
 * because tool arguments come from an LLM and are untrusted.
 */
public class RunDao {

    private final Database database;

    public RunDao(Database database) {
        this.database = database;
    }

    /**
     * Inserts a new run into the database.
     */
    public void insert(Task task) {
        String sql = "INSERT INTO runs (id, objective, status, created_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, task.id());
            ps.setString(2, task.objective());
            ps.setString(3, task.status().name());
            ps.setString(4, task.createdAt().toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert run: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the status of an existing run.
     */
    public void updateStatus(String runId, TaskStatus status) {
        String sql = "UPDATE runs SET status = ? WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, runId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update run status: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a run by its ID. Returns null if not found.
     */
    public Task findById(String runId) {
        String sql = "SELECT id, objective, status, created_at FROM runs WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, runId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find run: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all runs in the database (for listing/display purposes).
     */
    public List<Task> findAll() {
        String sql = "SELECT id, objective, status, created_at FROM runs ORDER BY created_at DESC";
        List<Task> runs = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                runs.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to list runs: " + e.getMessage(), e);
        }
        return runs;
    }

    /**
     * Converts a ResultSet row into a Task record.
     */
    private Task mapRow(ResultSet rs) throws SQLException {
        return new Task(
                rs.getString("id"),
                rs.getString("objective"),
                TaskStatus.valueOf(rs.getString("status")),
                List.of(),  // subtasks are loaded separately by SubTaskDao
                Instant.parse(rs.getString("created_at"))
        );
    }
}

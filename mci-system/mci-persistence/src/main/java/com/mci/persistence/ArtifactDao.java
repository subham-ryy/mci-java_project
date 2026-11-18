package com.mci.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'artifacts' table.
 * Tracks files that agents create in the workspace during execution.
 */
public class ArtifactDao {

    private final Database database;

    public ArtifactDao(Database database) {
        this.database = database;
    }

    /**
     * Records a file artifact produced during a subtask.
     */
    public void insert(String runId, String subtaskId, String filePath) {
        String sql = "INSERT INTO artifacts (run_id, subtask_id, file_path, created_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, runId);
            ps.setString(2, subtaskId);
            ps.setString(3, filePath);
            ps.setString(4, Instant.now().toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert artifact: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all artifacts for a given run.
     */
    public List<ArtifactEntry> findByRunId(String runId) {
        String sql = "SELECT id, run_id, subtask_id, file_path, created_at FROM artifacts WHERE run_id = ? ORDER BY id";
        List<ArtifactEntry> entries = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new ArtifactEntry(
                            rs.getLong("id"),
                            rs.getString("run_id"),
                            rs.getString("subtask_id"),
                            rs.getString("file_path"),
                            rs.getString("created_at")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query artifacts: " + e.getMessage(), e);
        }
        return entries;
    }

    /**
     * Simple data carrier for artifact entries.
     */
    public record ArtifactEntry(
            long id,
            String runId,
            String subtaskId,
            String filePath,
            String createdAt
    ) {}
}

package com.mci.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple helper class for SQLite database connections and setup.
 *
 * Usage:
 *   Database db = new Database("./data/mci.db");
 *   db.initSchema();                       // creates tables if they don't exist
 *   Connection conn = db.getConnection();  // use in DAO methods
 *
 * Nothing fancy — just plain JDBC with SQLite.
 */
public class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private final String jdbcUrl;

    /**
     * Creates a Database helper pointing to a SQLite file.
     *
     * @param dbFilePath path to the .db file, e.g. "./data/mci.db"
     */
    public Database(String dbFilePath) {
        // Make sure the parent directory exists so SQLite can create the file
        File dbFile = new File(dbFilePath);
        if (dbFile.getParentFile() != null) {
            dbFile.getParentFile().mkdirs();
        }

        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
        log.info("Database configured at: {}", dbFilePath);
    }

    /**
     * Opens and returns a new connection to the SQLite database.
     * Caller is responsible for closing it (use try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Reads schema.sql from the classpath and executes it.
     * Safe to call multiple times — all statements use CREATE IF NOT EXISTS.
     */
    public void initSchema() {
        String sql = loadSchemaFile();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Split by semicolons and execute each statement
            String[] statements = sql.split(";");
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }

            log.info("Database schema initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the schema.sql file from the resources folder.
     */
    private String loadSchemaFile() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql not found in classpath");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comment-only lines
                if (!line.trim().startsWith("--")) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema.sql: " + e.getMessage(), e);
        }
    }
}

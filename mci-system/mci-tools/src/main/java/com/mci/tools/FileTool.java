package com.mci.tools;

import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

/**
 * Simple tool for reading and writing files in the workspace.
 *
 * Supports two operations:
 *   - "file.read"  → reads a file and returns its contents
 *   - "file.write" → writes content to a file
 *
 * All file paths are relative to a base workspace directory
 * so agents can't access files outside their workspace.
 */
public class FileTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(FileTool.class);

    private final String baseDir;  // e.g. "./workspace"

    public FileTool(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String name() {
        return "file";  // handles both "file.read" and "file.write"
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String action = request.toolName();  // "file.read" or "file.write"

        if ("file.read".equals(action)) {
            return readFile(request);
        } else if ("file.write".equals(action)) {
            return writeFile(request);
        } else {
            return ToolResponse.error("Unknown file action: " + action);
        }
    }

    /**
     * Reads a file from the workspace and returns its contents.
     * Expects argument: "path" (relative to workspace, e.g. "run-001/data.txt")
     */
    private ToolResponse readFile(ToolRequest request) {
        String path = request.getString("path");
        if (path == null) {
            return ToolResponse.error("Missing 'path' argument");
        }

        File file = new File(baseDir, path);

        // Security check: make sure the resolved path is still inside the workspace
        if (!isInsideWorkspace(file)) {
            return ToolResponse.denied("Path escapes workspace: " + path);
        }

        if (!file.exists()) {
            return ToolResponse.error("File not found: " + path);
        }

        try {
            Scanner scanner = new Scanner(file);
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine());
                if (scanner.hasNextLine()) {
                    content.append("\n");
                }
            }
            scanner.close();

            log.info("Read file: {} ({} bytes)", path, content.length());
            return ToolResponse.success(content.toString());

        } catch (Exception e) {
            return ToolResponse.error("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Writes content to a file in the workspace.
     * Expects arguments: "path" and "content"
     */
    private ToolResponse writeFile(ToolRequest request) {
        String path = request.getString("path");
        String content = request.getString("content");

        if (path == null) {
            return ToolResponse.error("Missing 'path' argument");
        }
        if (content == null) {
            return ToolResponse.error("Missing 'content' argument");
        }

        File file = new File(baseDir, path);

        // Security check
        if (!isInsideWorkspace(file)) {
            return ToolResponse.denied("Path escapes workspace: " + path);
        }

        try {
            // Create parent directories if needed
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();

            log.info("Wrote file: {} ({} bytes)", path, content.length());
            return ToolResponse.success("File written: " + path);

        } catch (Exception e) {
            return ToolResponse.error("Failed to write file: " + e.getMessage());
        }
    }

    /**
     * Checks that the file path doesn't escape the workspace using ".." tricks.
     * This is a simple security check — agents shouldn't access files outside their workspace.
     */
    private boolean isInsideWorkspace(File file) {
        try {
            // getCanonicalPath resolves ".." and symlinks to the real path
            String filePath = file.getCanonicalPath();
            String basePath = new File(baseDir).getCanonicalPath();
            return filePath.startsWith(basePath);
        } catch (Exception e) {
            return false;  // if we can't resolve, deny
        }
    }
}

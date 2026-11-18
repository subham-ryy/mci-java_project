package com.mci.tools;

import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * Dedicated tool for safely saving and writing files into the workspace run folder.
 *
 * Implements strict path validation to ensure files stay inside the assigned workspace sandbox.
 */
public class FileWriterTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(FileWriterTool.class);

    private final String baseDir; // e.g. "./workspace"

    public FileWriterTool(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String name() {
        return "file.write";
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String path = request.getString("path");
        String content = request.getString("content");

        if (path == null || path.isBlank()) {
            return ToolResponse.error("Missing required parameter: 'path'");
        }
        if (content == null) {
            return ToolResponse.error("Missing required parameter: 'content'");
        }

        File targetFile = new File(baseDir, path);

        // Security sandbox validation
        if (!isInsideWorkspace(targetFile)) {
            log.warn("Security violation: Path escapes workspace: {}", path);
            return ToolResponse.denied("Path escapes workspace sandbox: " + path);
        }

        try {
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            long bytes = targetFile.length();
            log.info("Successfully wrote {} bytes to file: {}", bytes, path);
            return ToolResponse.success("File saved: " + path + " (" + bytes + " bytes)");

        } catch (Exception e) {
            log.error("Failed writing file {}: {}", path, e.getMessage());
            return ToolResponse.error("Failed to write file: " + e.getMessage());
        }
    }

    private boolean isInsideWorkspace(File file) {
        try {
            String filePath = file.getCanonicalPath();
            String basePath = new File(baseDir).getCanonicalPath();
            return filePath.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }
}


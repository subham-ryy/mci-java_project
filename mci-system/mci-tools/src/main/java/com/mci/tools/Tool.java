package com.mci.tools;

import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;

/**
 * Interface that all tools must implement.
 * Each tool has a name (like "file.read") and an execute method.
 */
public interface Tool {

    /** Returns the tool name, e.g. "file.read", "file.write" */
    String name();

    /** Executes the tool with the given request and returns a response. */
    ToolResponse execute(ToolRequest request);
}

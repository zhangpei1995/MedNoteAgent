package org.med.note.agent.runtime;

import org.med.note.agent.tool.ToolResult;

/**
 * Pairing of the audit record and optional successful tool result.
 */
public record ToolExecutionResult(ToolCallRecord record, ToolResult result) {
    public boolean succeeded() {
        return result != null;
    }
}

package org.med.note.agent.tool;

/**
 * One replaceable step in the MedNote agent pipeline.
 */
public interface AgentTool {
    /**
     * Executes the tool with the current immutable context and returns only the fields it wants to update.
     */
    ToolResult execute(ToolContext context);
}

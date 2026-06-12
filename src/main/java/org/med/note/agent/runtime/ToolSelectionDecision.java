package org.med.note.agent.runtime;

import org.med.note.agent.tool.AgentTool;
import org.med.note.agent.tool.AgentToolDescriptor;

import java.util.List;

/**
 * Planner output for the next dynamic tool action.
 */
public record ToolSelectionDecision(
        AgentTool selectedTool,
        AgentToolDescriptor selectedDescriptor,
        List<String> candidateTools,
        List<String> unloadedTools,
        List<String> skippedTools,
        String reason,
        String stopReason,
        double confidence,
        boolean requiresHumanReview
) {
    public boolean hasSelection() {
        return selectedTool != null;
    }
}

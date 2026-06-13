package org.med.note.agent.runtime;

import org.med.note.agent.tool.AgentTool;
import org.med.note.agent.tool.AgentToolDescriptor;

import java.util.List;

/**
 * One planner decision that may contain several dependency-ready tools.
 */
public record ToolSelectionBatch(
        List<SelectedTool> selectedTools,
        List<String> candidateTools,
        List<String> unloadedTools,
        List<String> skippedTools,
        String reason,
        String stopReason,
        double confidence,
        boolean requiresHumanReview,
        boolean parallel
) {
    public boolean hasSelection() {
        return selectedTools != null && !selectedTools.isEmpty();
    }

    public String firstSelectedToolName() {
        return hasSelection() ? selectedTools.get(0).descriptor().name() : "";
    }

    public List<String> selectedToolNames() {
        return hasSelection()
                ? selectedTools.stream().map(selectedTool -> selectedTool.descriptor().name()).toList()
                : List.of();
    }

    public record SelectedTool(AgentTool tool, AgentToolDescriptor descriptor) {
    }
}

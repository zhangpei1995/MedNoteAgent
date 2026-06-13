package org.med.note.agent.runtime;

import org.med.note.agent.tool.AgentTool;
import org.med.note.agent.tool.AgentToolDescriptor;
import org.med.note.agent.tool.AgentToolRegistry;
import org.med.note.agent.tool.ToolContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns session-level dynamic tool selection.
 *
 * <p>The planner re-ranks tools after every tool result, so later context changes can append
 * new tools. Executed tools are treated as unloaded for the current session to avoid loops.</p>
 */
@Component
public class AgentToolPlanner {

    private static final int MAX_CANDIDATES = 8;

    private final AgentToolRegistry toolRegistry;

    public AgentToolPlanner(AgentToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolSelectionDecision selectNext(ToolContext context, Set<String> executedToolNames) {
        ToolSelectionBatch batch = selectNextBatch(context, executedToolNames);
        if (!batch.hasSelection()) {
            return new ToolSelectionDecision(
                    null,
                    null,
                    batch.candidateTools(),
                    batch.unloadedTools(),
                    batch.skippedTools(),
                    batch.reason(),
                    batch.stopReason(),
                    batch.confidence(),
                    batch.requiresHumanReview()
            );
        }
        ToolSelectionBatch.SelectedTool firstSelection = batch.selectedTools().get(0);
        return new ToolSelectionDecision(
                firstSelection.tool(),
                firstSelection.descriptor(),
                batch.candidateTools(),
                batch.unloadedTools(),
                batch.skippedTools(),
                batch.reason(),
                batch.stopReason(),
                batch.confidence(),
                batch.requiresHumanReview()
        );
    }

    public ToolSelectionBatch selectNextBatch(ToolContext context, Set<String> executedToolNames) {
        Set<String> unloadedTools = executedToolNames == null ? Set.of() : executedToolNames;
        List<AgentTool> candidates = toolRegistry.rankTools(context, unloadedTools, MAX_CANDIDATES);
        List<String> candidateNames = candidates.stream()
                .map(tool -> toolRegistry.describe(tool).name())
                .toList();
        List<String> skippedTools = skippedTools(candidateNames, unloadedTools);
        List<String> unloadedToolNames = unloadedTools.stream().sorted().toList();

        if (candidates.isEmpty()) {
            return new ToolSelectionBatch(
                    List.of(),
                    candidateNames,
                    unloadedToolNames,
                    skippedTools,
                    "no-candidate-after-dynamic-pruning",
                    "no_more_candidate_tools",
                    0.0,
                    false,
                    false
            );
        }

        List<ToolSelectionBatch.SelectedTool> readyTools = candidates.stream()
                .map(tool -> new ToolSelectionBatch.SelectedTool(tool, toolRegistry.describe(tool)))
                .filter(selection -> dependenciesSatisfied(selection.descriptor(), unloadedTools))
                .toList();
        if (readyTools.isEmpty()) {
            return new ToolSelectionBatch(
                    List.of(),
                    candidateNames,
                    unloadedToolNames,
                    skippedTools,
                    "candidate-tools-waiting-for-dependencies",
                    "waiting_for_dependencies",
                    0.0,
                    false,
                    false
            );
        }

        ToolSelectionBatch.SelectedTool firstSelection = readyTools.get(0);
        List<ToolSelectionBatch.SelectedTool> selectedTools = firstSelection.descriptor().parallelizable()
                ? readyTools.stream()
                .filter(selection -> selection.descriptor().parallelizable())
                .filter(selection -> canShareBatch(firstSelection.descriptor(), selection.descriptor()))
                .toList()
                : List.of(firstSelection);
        return new ToolSelectionBatch(
                selectedTools,
                candidateNames,
                unloadedToolNames,
                skippedTools,
                selectedTools.size() > 1 ? "dynamic-rerank-ready-parallel-batch" : "dynamic-rerank-after-context-update",
                "continue",
                selectedTools.stream()
                        .mapToDouble(selection -> confidence(context, selection.descriptor()))
                        .average()
                        .orElse(0.0),
                selectedTools.stream().anyMatch(selection -> requiresHumanReview(context, selection.descriptor())),
                selectedTools.size() > 1
        );
    }

    private boolean dependenciesSatisfied(AgentToolDescriptor descriptor, Set<String> executedToolNames) {
        Set<String> executed = executedToolNames == null ? Set.of() : executedToolNames;
        return executed.containsAll(descriptor.dependsOn());
    }

    private boolean canShareBatch(AgentToolDescriptor firstDescriptor, AgentToolDescriptor candidateDescriptor) {
        return candidateDescriptor.order() >= firstDescriptor.order()
                && !candidateDescriptor.dependsOn().contains(firstDescriptor.name())
                && !firstDescriptor.dependsOn().contains(candidateDescriptor.name());
    }

    private double confidence(ToolContext context, AgentToolDescriptor selectedDescriptor) {
        double score = selectedDescriptor.required() ? 0.45 : 0.30;
        if (!context.taskKeywords().isEmpty()) {
            score += 0.15;
        }
        if (context.intent() != null && !context.intent().isBlank()) {
            score += 0.10;
        }
        if (!context.queryKeywords().isEmpty()) {
            score += 0.10;
        }
        if (!context.evidence().isEmpty()) {
            score += 0.10;
        }
        if (context.riskLevel() != null && !context.riskLevel().isBlank()) {
            score += 0.05;
        }
        return Math.min(0.95, score);
    }

    private boolean requiresHumanReview(ToolContext context, AgentToolDescriptor selectedDescriptor) {
        return "answer_generation".equals(selectedDescriptor.name()) && "HIGH".equals(context.riskLevel());
    }

    private List<String> skippedTools(List<String> candidateTools, Set<String> unloadedTools) {
        Set<String> skipped = new LinkedHashSet<>();
        Set<String> candidateSet = new LinkedHashSet<>(candidateTools);
        Set<String> unloadedSet = unloadedTools == null ? Set.of() : unloadedTools;
        for (AgentToolDescriptor descriptor : toolRegistry.listDescriptors()) {
            if (!candidateSet.contains(descriptor.name()) && !unloadedSet.contains(descriptor.name())) {
                skipped.add(descriptor.name());
            }
        }
        return skipped.stream().toList();
    }
}

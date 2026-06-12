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
        Set<String> unloadedTools = executedToolNames == null ? Set.of() : executedToolNames;
        List<AgentTool> candidates = toolRegistry.rankTools(context, unloadedTools, MAX_CANDIDATES);
        List<String> candidateNames = candidates.stream()
                .map(tool -> toolRegistry.describe(tool).name())
                .toList();
        List<String> skippedTools = skippedTools(candidateNames, unloadedTools);
        List<String> unloadedToolNames = unloadedTools.stream().sorted().toList();

        if (candidates.isEmpty()) {
            return new ToolSelectionDecision(
                    null,
                    null,
                    List.of(),
                    unloadedToolNames,
                    skippedTools,
                    "no-candidate-after-dynamic-pruning",
                    "no_more_candidate_tools",
                    0.0,
                    false
            );
        }

        AgentTool selectedTool = candidates.get(0);
        AgentToolDescriptor selectedDescriptor = toolRegistry.describe(selectedTool);
        return new ToolSelectionDecision(
                selectedTool,
                selectedDescriptor,
                candidateNames,
                unloadedToolNames,
                skippedTools,
                "dynamic-rerank-after-context-update",
                "continue",
                confidence(context, selectedDescriptor),
                requiresHumanReview(context, selectedDescriptor)
        );
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

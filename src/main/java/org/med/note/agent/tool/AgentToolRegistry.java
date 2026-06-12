package org.med.note.agent.tool;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Discovers Spring-managed {@link AgentTool} implementations and selects tools for one agent run.
 *
 * <p>The registry intentionally owns metadata lookup and scoring so {@code MedNoteAgent}
 * stays focused on orchestration rather than tool-selection details.</p>
 */
@Component
public class AgentToolRegistry {

    private final ObjectProvider<AgentTool> toolProvider;

    public AgentToolRegistry(ObjectProvider<AgentTool> toolProvider) {
        this.toolProvider = toolProvider;
    }

    public List<AgentTool> rankTools(ToolContext context, Set<String> excludedToolNames, int maxTools) {
        String normalizedTask = normalize(String.join(" ",
                context.topic(),
                context.input(),
                String.join(" ", context.taskKeywords()),
                context.intent(),
                context.rewrittenQuery(),
                String.join(" ", context.queryKeywords())
        ));
        Set<String> safeExcludedToolNames = excludedToolNames == null ? Set.of() : excludedToolNames;
        return toolProvider.stream()
                .map(tool -> {
                    AgentToolDescriptor descriptor = describe(tool);
                    return new ScoredTool(tool, descriptor, score(descriptor, normalizedTask));
                })
                .filter(scoredTool -> !safeExcludedToolNames.contains(scoredTool.descriptor().name()))
                .filter(scoredTool -> scoredTool.descriptor().required() || scoredTool.score() > 0)
                .sorted(Comparator
                        .comparing((ScoredTool scoredTool) -> scoredTool.descriptor().order())
                        .thenComparing(Comparator.comparingInt(ScoredTool::score).reversed())
                        .thenComparing(scoredTool -> scoredTool.descriptor().name()))
                .limit(Math.max(1, maxTools))
                .map(ScoredTool::tool)
                .toList();
    }

    public List<AgentToolDescriptor> listDescriptors() {
        return toolProvider.stream()
                .map(this::describe)
                .sorted(Comparator.comparing(AgentToolDescriptor::order).thenComparing(AgentToolDescriptor::name))
                .toList();
    }

    public AgentToolDescriptor describe(AgentTool tool) {
        AgentToolDefinition definition = tool.getClass().getAnnotation(AgentToolDefinition.class);
        if (definition == null) {
            return new AgentToolDescriptor(tool.getClass().getSimpleName(), "未声明描述", "extension", Integer.MAX_VALUE, false, List.of(), List.of());
        }
        return new AgentToolDescriptor(
                definition.name(),
                definition.description(),
                definition.phase(),
                definition.order(),
                definition.required(),
                List.of(definition.keywordHints()),
                List.of(definition.triggers())
        );
    }

    private int score(AgentToolDescriptor descriptor, String normalizedTask) {
        int score = descriptor.required() ? 1 : 0;
        for (String keywordHint : descriptor.keywordHints()) {
            if (normalizedTask.contains(normalize(keywordHint))) {
                score += 3;
            }
        }
        for (String trigger : descriptor.triggers()) {
            if (normalizedTask.contains(normalize(trigger))) {
                score += 2;
            }
        }
        if (normalizedTask.contains(normalize(descriptor.name()))) {
            score += 3;
        }
        return score;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[，。；、：,.?？!！]", " ");
    }

    private record ScoredTool(AgentTool tool, AgentToolDescriptor descriptor, int score) {
    }
}

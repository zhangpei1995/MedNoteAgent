package org.med.note.agent.runtime;

import org.med.note.agent.tool.ToolContext;
import org.med.note.agent.tool.ToolResult;
import org.med.note.knowledge.evidence.EvidenceChunk;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * Applies partial tool outputs to the immutable tool context.
 */
@Component
public class AgentContextMerger {

    public ToolContext merge(ToolContext context, ToolResult result) {
        String topic = blank(result.topic()) ? context.topic() : result.topic();
        List<String> taskKeywords = result.taskKeywords().isEmpty() ? context.taskKeywords() : result.taskKeywords();
        String intent = blank(result.intent()) ? context.intent() : result.intent();
        String rewrittenQuery = blank(result.rewrittenQuery()) ? context.rewrittenQuery() : result.rewrittenQuery();
        List<String> queryKeywords = result.queryKeywords().isEmpty() ? context.queryKeywords() : result.queryKeywords();
        List<EvidenceChunk> evidence = result.evidence().isEmpty() ? context.evidence() : result.evidence();
        String riskLevel = blank(result.riskLevel()) ? context.riskLevel() : result.riskLevel();
        String finalAnswer = blank(result.finalAnswer()) ? context.finalAnswer() : result.finalAnswer();

        HashMap<String, Object> memory = new HashMap<>(context.memory());
        memory.put(result.toolName(), result.metadata());
        return new ToolContext(topic, context.input(), taskKeywords, intent, rewrittenQuery, queryKeywords, evidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext snapshot(ToolContext context) {
        return new ToolContext(
                context.topic(),
                context.input(),
                context.taskKeywords(),
                context.intent(),
                context.rewrittenQuery(),
                context.queryKeywords(),
                context.evidence(),
                context.riskLevel(),
                context.finalAnswer(),
                new HashMap<>(context.memory())
        );
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

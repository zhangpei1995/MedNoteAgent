package org.med.note.agent.tool;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;
import java.util.Map;

/**
 * Immutable state shared across tools during one agent run.
 */
public record ToolContext(
        String topic,
        String input,
        List<String> taskKeywords,
        String intent,
        String rewrittenQuery,
        List<String> queryKeywords,
        List<EvidenceChunk> evidence,
        String riskLevel,
        String finalAnswer,
        Map<String, Object> memory
) {
    public ToolContext withTopic(String newTopic) {
        return new ToolContext(newTopic, input, taskKeywords, intent, rewrittenQuery, queryKeywords, evidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext withTaskKeywords(List<String> newTaskKeywords) {
        return new ToolContext(topic, input, safeList(newTaskKeywords), intent, rewrittenQuery, queryKeywords, evidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext withIntent(String newIntent) {
        return new ToolContext(topic, input, taskKeywords, newIntent, rewrittenQuery, queryKeywords, evidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext withRewrittenQuery(String newRewrittenQuery) {
        return new ToolContext(topic, input, taskKeywords, intent, newRewrittenQuery, queryKeywords, evidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext withQueryKeywords(List<String> newQueryKeywords) {
        return new ToolContext(topic, input, taskKeywords, intent, rewrittenQuery, safeList(newQueryKeywords), evidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext withEvidence(List<EvidenceChunk> newEvidence) {
        return new ToolContext(topic, input, taskKeywords, intent, rewrittenQuery, queryKeywords, newEvidence, riskLevel, finalAnswer, memory);
    }

    public ToolContext withRiskLevel(String newRiskLevel) {
        return new ToolContext(topic, input, taskKeywords, intent, rewrittenQuery, queryKeywords, evidence, newRiskLevel, finalAnswer, memory);
    }

    public ToolContext withFinalAnswer(String newFinalAnswer) {
        return new ToolContext(topic, input, taskKeywords, intent, rewrittenQuery, queryKeywords, evidence, riskLevel, newFinalAnswer, memory);
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }
}

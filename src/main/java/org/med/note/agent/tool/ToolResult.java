package org.med.note.agent.tool;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;
import java.util.Map;

/**
 * Partial state update emitted by a tool. Empty fields mean “keep the current context value”.
 */
public record ToolResult(
        String toolName,
        String summary,
        String topic,
        List<String> taskKeywords,
        String intent,
        String rewrittenQuery,
        List<String> queryKeywords,
        List<EvidenceChunk> evidence,
        String riskLevel,
        String finalAnswer,
        String content,
        Map<String, Object> metadata
) {
    public static ToolResult of(
            String toolName,
            String summary,
            String topic,
            List<String> taskKeywords,
            String intent,
            String rewrittenQuery,
            List<String> queryKeywords,
            List<EvidenceChunk> evidence,
            String riskLevel,
            String finalAnswer,
            String content,
            Map<String, Object> metadata
    ) {
        return new ToolResult(
                toolName,
                summary,
                topic,
                safeList(taskKeywords),
                intent,
                rewrittenQuery,
                safeList(queryKeywords),
                evidence == null ? List.of() : evidence,
                riskLevel,
                finalAnswer,
                content,
                metadata == null ? Map.of() : metadata
        );
    }

    private static List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }
}

package org.med.note.agent.retrieval;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Structured retrieval input produced by request planning and tool context.
 */
public record EvidenceRetrievalRequest(
        String topic,
        String query,
        List<String> queryKeywords,
        String intent,
        String riskLevel,
        int limit
) {
    public EvidenceRetrievalRequest {
        topic = safe(topic);
        query = safe(query);
        queryKeywords = queryKeywords == null ? List.of() : deduplicate(queryKeywords);
        intent = safe(intent);
        riskLevel = safe(riskLevel);
        limit = Math.max(1, limit);
    }

    public String searchableText() {
        return (topic + " " + query + " " + String.join(" ", queryKeywords)).trim();
    }

    private static List<String> deduplicate(List<String> values) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                unique.add(value.trim());
            }
        }
        return List.copyOf(unique);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

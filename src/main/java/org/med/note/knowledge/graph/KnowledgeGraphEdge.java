package org.med.note.knowledge.graph;

import java.time.Instant;
import java.util.Map;

/**
 * Directed relationship between two graph nodes.
 */
public record KnowledgeGraphEdge(
        String sourceNodeId,
        String targetNodeId,
        String type,
        double weight,
        String evidenceId,
        Map<String, Object> properties,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeGraphEdge of(
            String sourceNodeId,
            String targetNodeId,
            String type,
            double weight,
            String evidenceId,
            Map<String, Object> properties
    ) {
        Instant now = Instant.now();
        return new KnowledgeGraphEdge(
                sourceNodeId,
                targetNodeId,
                type,
                weight,
                evidenceId == null ? "" : evidenceId,
                properties == null ? Map.of() : Map.copyOf(properties),
                now,
                now
        );
    }
}

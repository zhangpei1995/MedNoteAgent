package org.med.note.knowledge.graph;

import java.time.Instant;
import java.util.Map;

/**
 * One normalized graph vertex. Node ids are stable business ids, not database row ids.
 */
public record KnowledgeGraphNode(
        String id,
        String type,
        String name,
        String canonicalName,
        String sourceId,
        Map<String, Object> properties,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeGraphNode of(String id, String type, String name, String canonicalName, String sourceId, Map<String, Object> properties) {
        Instant now = Instant.now();
        return new KnowledgeGraphNode(
                id,
                type,
                name,
                canonicalName == null || canonicalName.isBlank() ? name : canonicalName,
                sourceId == null ? "" : sourceId,
                properties == null ? Map.of() : Map.copyOf(properties),
                now,
                now
        );
    }
}

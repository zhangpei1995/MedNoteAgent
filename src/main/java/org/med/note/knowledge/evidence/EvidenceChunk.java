package org.med.note.knowledge.evidence;

public record EvidenceChunk(
        String id,
        String drugName,
        String section,
        String content,
        double score
) {
}

package org.med.note.domain;

public record EvidenceChunk(
        String id,
        String drugName,
        String section,
        String content,
        double score
) {
}

package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;
import java.util.Map;

/**
 * Ranked evidence plus diagnostics used for audit and performance tuning.
 */
public record EvidenceRetrievalResult(
        EvidenceRetrievalMode mode,
        List<EvidenceChunk> evidence,
        List<EvidenceCandidate> candidates,
        Map<String, Object> metadata
) {
    public EvidenceRetrievalResult {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

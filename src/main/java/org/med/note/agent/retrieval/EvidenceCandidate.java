package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;

/**
 * A retrieval candidate with source-specific ranking metadata.
 */
public record EvidenceCandidate(
        EvidenceChunk chunk,
        String channel,
        int rank,
        double score,
        List<String> matchedTerms
) {
    public EvidenceCandidate {
        matchedTerms = matchedTerms == null ? List.of() : List.copyOf(matchedTerms);
    }
}

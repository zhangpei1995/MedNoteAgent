package org.med.note.agent.retrieval;

import java.util.List;

/**
 * Combines candidates from multiple recall channels into one stable evidence ranking.
 */
public interface EvidenceRankFusion {

    List<EvidenceCandidate> fuse(List<EvidenceCandidate> candidates, int limit);
}

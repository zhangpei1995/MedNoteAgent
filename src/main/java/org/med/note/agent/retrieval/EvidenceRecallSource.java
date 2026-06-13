package org.med.note.agent.retrieval;

import java.util.List;

/**
 * One independent evidence recall path, such as keyword search, semantic search, or graph search.
 */
public interface EvidenceRecallSource {

    String name();

    List<EvidenceCandidate> recall(EvidenceRetrievalRequest request, int limit);
}

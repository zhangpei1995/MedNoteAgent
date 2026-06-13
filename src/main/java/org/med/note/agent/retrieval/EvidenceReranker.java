package org.med.note.agent.retrieval;

import java.util.List;

/**
 * Optional expensive reranking step used only for accurate retrieval.
 */
public interface EvidenceReranker {

    List<EvidenceCandidate> rerank(EvidenceRetrievalRequest request, List<EvidenceCandidate> candidates, int limit);
}

package org.med.note.agent.retrieval;

/**
 * Rich retrieval entry point used by tools that need diagnostics beyond the evidence list.
 */
public interface EvidenceSearchService {

    EvidenceRetrievalResult retrieve(EvidenceRetrievalRequest request);
}

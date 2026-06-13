package org.med.note.agent.retrieval;

/**
 * Chooses how deep the retrieval pipeline should run for a request.
 */
public interface EvidenceRetrievalStrategy {

    EvidenceRetrievalMode chooseMode(EvidenceRetrievalRequest request, ListSignal signal);

    record ListSignal(
            int fastCandidateCount,
            boolean hasExplicitDrug,
            boolean hasExplicitSection,
            boolean highRisk
    ) {
    }
}

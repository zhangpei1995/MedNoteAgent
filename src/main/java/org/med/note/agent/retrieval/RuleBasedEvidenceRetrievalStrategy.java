package org.med.note.agent.retrieval;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Selects the cheapest retrieval depth that is likely to answer the question.
 */
@Component
public class RuleBasedEvidenceRetrievalStrategy implements EvidenceRetrievalStrategy {

    @Override
    public EvidenceRetrievalMode chooseMode(EvidenceRetrievalRequest request, ListSignal signal) {
        if (signal.highRisk() || highRiskIntent(request.intent()) || containsAny(request.searchableText(), "合并", "一起吃", "相互作用")) {
            return EvidenceRetrievalMode.ACCURATE;
        }
        if (signal.fastCandidateCount() < request.limit() || !signal.hasExplicitDrug() || !signal.hasExplicitSection()) {
            return EvidenceRetrievalMode.BALANCED;
        }
        return EvidenceRetrievalMode.FAST;
    }

    private boolean highRiskIntent(String intent) {
        return List.of("CONTRAINDICATION", "SPECIAL_POPULATION", "ADVERSE_REACTION").contains(intent);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text != null && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

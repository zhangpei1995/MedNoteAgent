package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic reranker used until a model reranker is connected.
 */
@Component
public class LocalEvidenceReranker implements EvidenceReranker {

    @Override
    public List<EvidenceCandidate> rerank(EvidenceRetrievalRequest request, List<EvidenceCandidate> candidates, int limit) {
        List<EvidenceCandidate> reranked = candidates.stream()
                .map(candidate -> boost(request, candidate))
                .sorted(Comparator.comparingDouble(EvidenceCandidate::score).reversed())
                .limit(limit)
                .toList();

        List<EvidenceCandidate> withRank = new ArrayList<>();
        for (int index = 0; index < reranked.size(); index++) {
            EvidenceCandidate candidate = reranked.get(index);
            withRank.add(new EvidenceCandidate(candidate.chunk(), "local-rerank", index + 1, candidate.score(), candidate.matchedTerms()));
        }
        return withRank;
    }

    private EvidenceCandidate boost(EvidenceRetrievalRequest request, EvidenceCandidate candidate) {
        EvidenceChunk chunk = candidate.chunk();
        double score = candidate.score();
        String text = request.searchableText();
        if (text.contains(chunk.drugName())) {
            score += 2.0;
        }
        if (text.contains(chunk.section())) {
            score += 1.5;
        }
        if ("HIGH".equalsIgnoreCase(request.riskLevel()) && List.of("禁忌", "注意事项", "不良反应").contains(chunk.section())) {
            score += 1.0;
        }
        return new EvidenceCandidate(
                new EvidenceChunk(chunk.id(), chunk.drugName(), chunk.section(), chunk.content(), score),
                candidate.channel(),
                candidate.rank(),
                score,
                candidate.matchedTerms()
        );
    }
}

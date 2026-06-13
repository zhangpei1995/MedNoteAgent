package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reciprocal Rank Fusion for heterogeneous recall channels.
 */
@Component
public class RrfEvidenceRankFusion implements EvidenceRankFusion {

    private static final int RRF_K = 60;
    private static final Map<String, Double> CHANNEL_WEIGHTS = Map.of(
            "keyword", 1.2,
            "semantic-synonym", 1.0,
            "local-rerank", 1.4
    );

    @Override
    public List<EvidenceCandidate> fuse(List<EvidenceCandidate> candidates, int limit) {
        Map<String, Accumulator> scores = new LinkedHashMap<>();
        for (EvidenceCandidate candidate : candidates == null ? List.<EvidenceCandidate>of() : candidates) {
            if (candidate.chunk() == null || candidate.chunk().id() == null) {
                continue;
            }
            Accumulator accumulator = scores.computeIfAbsent(candidate.chunk().id(), id -> new Accumulator(candidate.chunk()));
            double weight = CHANNEL_WEIGHTS.getOrDefault(candidate.channel(), 1.0);
            int rank = candidate.rank() <= 0 ? 999 : candidate.rank();
            accumulator.score += weight / (RRF_K + rank);
            accumulator.channels.add(candidate.channel());
            accumulator.matchedTerms.addAll(candidate.matchedTerms());
        }

        return scores.values().stream()
                .sorted(Comparator.comparingDouble(Accumulator::score).reversed())
                .limit(limit)
                .map(Accumulator::toCandidate)
                .toList();
    }

    private static class Accumulator {
        private final EvidenceChunk chunk;
        private final Set<String> channels = new LinkedHashSet<>();
        private final Set<String> matchedTerms = new LinkedHashSet<>();
        private double score;

        private Accumulator(EvidenceChunk chunk) {
            this.chunk = chunk;
        }

        private double score() {
            return score;
        }

        private EvidenceCandidate toCandidate() {
            return new EvidenceCandidate(
                    new EvidenceChunk(chunk.id(), chunk.drugName(), chunk.section(), chunk.content(), score),
                    String.join("+", new ArrayList<>(channels)),
                    0,
                    score,
                    List.copyOf(matchedTerms)
            );
        }
    }
}

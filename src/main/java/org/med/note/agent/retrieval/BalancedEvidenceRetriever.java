package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Main evidence retriever for the agent.
 *
 * <p>The retriever first tries a fast keyword path, then upgrades to semantic
 * recall or local reranking only when the request shape needs it. This keeps
 * common instruction lookups fast while preserving a higher-quality path for
 * high-risk medication questions.</p>
 */
@Primary
@Service
public class BalancedEvidenceRetriever implements EvidenceRetriever, EvidenceSearchService {

    private final FixtureEvidenceRetriever keywordRecallSource;
    private final List<EvidenceRecallSource> recallSources;
    private final EvidenceRankFusion rankFusion;
    private final EvidenceRetrievalStrategy retrievalStrategy;
    private final EvidenceReranker reranker;

    public BalancedEvidenceRetriever(
            FixtureEvidenceRetriever keywordRecallSource,
            List<EvidenceRecallSource> recallSources,
            EvidenceRankFusion rankFusion,
            EvidenceRetrievalStrategy retrievalStrategy,
            EvidenceReranker reranker
    ) {
        this.keywordRecallSource = keywordRecallSource;
        this.recallSources = recallSources;
        this.rankFusion = rankFusion;
        this.retrievalStrategy = retrievalStrategy;
        this.reranker = reranker;
    }

    @Override
    public List<EvidenceChunk> search(String topic, String query, List<String> queryKeywords, int limit) {
        EvidenceRetrievalRequest request = new EvidenceRetrievalRequest(topic, query, queryKeywords, "", "", limit);
        return retrieve(request).evidence();
    }

    @Override
    public EvidenceRetrievalResult retrieve(EvidenceRetrievalRequest request) {
        long startedAt = System.nanoTime();
        List<EvidenceCandidate> keywordCandidates = keywordRecallSource.recall(request, Math.max(request.limit(), 8));
        EvidenceRetrievalMode mode = retrievalStrategy.chooseMode(request, signal(request, keywordCandidates));

        List<EvidenceCandidate> candidates = new ArrayList<>(keywordCandidates);
        if (mode != EvidenceRetrievalMode.FAST) {
            for (EvidenceRecallSource source : recallSources) {
                if (!keywordRecallSource.name().equals(source.name())) {
                    candidates.addAll(source.recall(request, Math.max(request.limit() * 2, 8)));
                }
            }
        }

        List<EvidenceCandidate> fused = rankFusion.fuse(candidates, Math.max(request.limit() * 2, request.limit()));
        if (mode == EvidenceRetrievalMode.ACCURATE) {
            fused = rankFusion.fuse(reranker.rerank(request, fused, Math.max(request.limit() * 2, request.limit())), request.limit());
        } else {
            fused = fused.stream()
                    .sorted(Comparator.comparingDouble(EvidenceCandidate::score).reversed())
                    .limit(request.limit())
                    .toList();
        }

        List<EvidenceChunk> evidence = fused.stream()
                .map(EvidenceCandidate::chunk)
                .limit(request.limit())
                .toList();

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        return new EvidenceRetrievalResult(
                mode,
                evidence,
                fused,
                Map.of(
                        "mode", mode.name(),
                        "elapsedMillis", elapsedMillis,
                        "candidateCount", candidates.size(),
                        "channels", candidates.stream().map(EvidenceCandidate::channel).distinct().toList(),
                        "fastCandidateCount", keywordCandidates.size()
                )
        );
    }

    private EvidenceRetrievalStrategy.ListSignal signal(EvidenceRetrievalRequest request, List<EvidenceCandidate> fastCandidates) {
        String text = request.searchableText();
        return new EvidenceRetrievalStrategy.ListSignal(
                fastCandidates.size(),
                keywordRecallSource.allEvidence().stream().map(EvidenceChunk::drugName).distinct().anyMatch(text::contains),
                keywordRecallSource.allEvidence().stream().map(EvidenceChunk::section).distinct().anyMatch(text::contains),
                "HIGH".equalsIgnoreCase(request.riskLevel()) || containsAny(text, "禁忌", "过敏", "孕妇", "儿童", "肝肾", "合并", "相互作用")
        );
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

package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Local semantic recall fallback based on medical synonym expansion.
 *
 * <p>This keeps the pipeline useful before a real embedding index is connected.
 * A future embedding implementation can replace this source without changing
 * {@link EvidenceRetriever} or the agent tools.</p>
 */
@Component
public class SemanticSynonymEvidenceRecallSource implements EvidenceRecallSource {

    private static final Map<String, List<String>> SEMANTIC_EXPANSIONS = Map.ofEntries(
            Map.entry("能不能吃", List.of("禁忌", "慎用", "注意事项", "服用可行性")),
            Map.entry("可以吃", List.of("禁忌", "慎用", "注意事项")),
            Map.entry("副作用", List.of("不良反应", "胃部不适", "恶心", "停药")),
            Map.entry("怎么吃", List.of("用法用量", "开水冲服", "一次", "一日")),
            Map.entry("孕妇", List.of("孕妇", "特殊人群", "医师指导")),
            Map.entry("儿童", List.of("儿童", "特殊人群", "医师指导")),
            Map.entry("老人", List.of("年老体弱", "老人", "特殊人群", "医师指导")),
            Map.entry("肝肾", List.of("肝肾功能", "医师", "注意事项")),
            Map.entry("一起吃", List.of("合并用药", "相互作用", "避免自行合并")),
            Map.entry("合用", List.of("合并用药", "相互作用", "避免自行合并")),
            Map.entry("忌口", List.of("忌烟", "忌酒", "辛辣", "生冷", "油腻"))
    );

    private final FixtureEvidenceRetriever fixtureEvidenceRetriever;

    public SemanticSynonymEvidenceRecallSource(FixtureEvidenceRetriever fixtureEvidenceRetriever) {
        this.fixtureEvidenceRetriever = fixtureEvidenceRetriever;
    }

    @Override
    public String name() {
        return "semantic-synonym";
    }

    @Override
    public List<EvidenceCandidate> recall(EvidenceRetrievalRequest request, int limit) {
        List<String> expandedTerms = expandedTerms(request);
        String normalizedQuery = normalize(request.searchableText() + " " + String.join(" ", expandedTerms));
        String explicitDrugName = explicitDrugName(normalizedQuery);

        List<EvidenceCandidate> ranked = fixtureEvidenceRetriever.allEvidence().stream()
                .filter(chunk -> explicitDrugName == null || chunk.drugName().equals(explicitDrugName))
                .map(chunk -> toCandidate(chunk, normalizedQuery, expandedTerms))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(EvidenceCandidate::score).reversed())
                .limit(limit)
                .toList();

        List<EvidenceCandidate> withRank = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            EvidenceCandidate candidate = ranked.get(index);
            withRank.add(new EvidenceCandidate(candidate.chunk(), candidate.channel(), index + 1, candidate.score(), candidate.matchedTerms()));
        }
        return withRank;
    }

    private EvidenceCandidate toCandidate(EvidenceChunk chunk, String normalizedQuery, List<String> expandedTerms) {
        String haystack = normalize(chunk.drugName() + " " + chunk.section() + " " + chunk.content());
        double score = 0;
        List<String> matches = new ArrayList<>();
        for (String term : expandedTerms) {
            String normalizedTerm = normalize(term);
            if (!normalizedTerm.isBlank() && haystack.contains(normalizedTerm)) {
                score += semanticWeight(term);
                matches.add(term);
            }
        }
        if (normalizedQuery.contains(normalize(chunk.drugName()))) {
            score += 2.0;
            matches.add(chunk.drugName());
        }
        return new EvidenceCandidate(
                new EvidenceChunk(chunk.id(), chunk.drugName(), chunk.section(), chunk.content(), score),
                name(),
                0,
                score,
                matches.stream().distinct().limit(8).toList()
        );
    }

    private List<String> expandedTerms(EvidenceRetrievalRequest request) {
        Set<String> terms = new LinkedHashSet<>(request.queryKeywords());
        String text = request.searchableText();
        for (Map.Entry<String, List<String>> entry : SEMANTIC_EXPANSIONS.entrySet()) {
            if (text.contains(entry.getKey())) {
                terms.addAll(entry.getValue());
            }
        }
        return List.copyOf(terms);
    }

    private double semanticWeight(String term) {
        if (List.of("禁忌", "不良反应", "用法用量", "注意事项", "合并用药").contains(term)) {
            return 1.3;
        }
        return 0.8;
    }

    private String explicitDrugName(String normalizedQuery) {
        return fixtureEvidenceRetriever.allEvidence().stream()
                .map(EvidenceChunk::drugName)
                .distinct()
                .filter(drugName -> normalizedQuery.contains(normalize(drugName)))
                .findFirst()
                .orElse(null);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[，。；、：,.?？!！]", " ");
    }
}

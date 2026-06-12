package org.med.note.service;

import org.med.note.domain.EvidenceChunk;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MockDrugKnowledgeBase {

    private static final List<EvidenceChunk> MOCK_EVIDENCE = List.of(
            new EvidenceChunk(
                    "mock-edt-indication",
                    "二冬汤颗粒",
                    "功能主治",
                    "养阴润肺。用于肺阴不足所致的咽干、干咳少痰、口燥咽痛等症状。",
                    0
            ),
            new EvidenceChunk(
                    "mock-edt-dosage",
                    "二冬汤颗粒",
                    "用法用量",
                    "开水冲服，一次 1 袋，一日 2 次。儿童、孕妇、年老体弱者应在医师指导下使用。",
                    0
            ),
            new EvidenceChunk(
                    "mock-edt-caution",
                    "二冬汤颗粒",
                    "注意事项",
                    "服药期间忌烟、酒及辛辣、生冷、油腻食物。症状持续或加重时应及时就医。",
                    0
            ),
            new EvidenceChunk(
                    "mock-edt-adverse",
                    "二冬汤颗粒",
                    "不良反应",
                    "少数患者可能出现胃部不适、恶心等反应，通常停药后可缓解。",
                    0
            ),
            new EvidenceChunk(
                    "mock-cmxf-indication",
                    "菖麻熄风颗粒",
                    "功能主治",
                    "平肝熄风、化痰通络。用于风痰阻络相关症状的辅助治疗。",
                    0
            ),
            new EvidenceChunk(
                    "mock-cmxf-caution",
                    "菖麻熄风颗粒",
                    "注意事项",
                    "运动员、孕妇、儿童及肝肾功能异常患者用药前应咨询医师。服药期间避免自行合并多种中成药。",
                    0
            ),
            new EvidenceChunk(
                    "mock-cmxf-contraindication",
                    "菖麻熄风颗粒",
                    "禁忌",
                    "对本品及所含成分过敏者禁用，过敏体质者慎用。",
                    0
            )
    );

    public List<EvidenceChunk> search(String topic, String query, int limit) {
        String normalized = normalize(topic + " " + query);
        String explicitDrugName = resolveExplicitDrugName(normalized);
        return MOCK_EVIDENCE.stream()
                .filter(chunk -> explicitDrugName == null || chunk.drugName().equals(explicitDrugName))
                .map(chunk -> new EvidenceChunk(
                        chunk.id(),
                        chunk.drugName(),
                        chunk.section(),
                        chunk.content(),
                        score(chunk, normalized)
                ))
                .filter(chunk -> chunk.score() > 0)
                .sorted(Comparator.comparingDouble(EvidenceChunk::score).reversed())
                .limit(limit)
                .toList();
    }

    private String resolveExplicitDrugName(String query) {
        return MOCK_EVIDENCE.stream()
                .map(EvidenceChunk::drugName)
                .distinct()
                .filter(drugName -> query.contains(normalize(drugName)))
                .findFirst()
                .orElse(null);
    }

    private double score(EvidenceChunk chunk, String query) {
        double score = 0;
        String haystack = normalize(chunk.drugName() + " " + chunk.section() + " " + chunk.content());

        if (query.contains(normalize(chunk.drugName()))) {
            score += 3.0;
        }
        if (query.contains(normalize(chunk.section()))) {
            score += 2.0;
        }

        for (String token : List.of("适应症", "功能", "主治", "用法", "用量", "禁忌", "注意", "不良", "反应", "孕妇", "儿童", "合并", "过敏")) {
            if (query.contains(token) && haystack.contains(token)) {
                score += 1.0;
            }
        }

        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && token.length() >= 2 && haystack.contains(token)) {
                score += 0.2;
            }
        }
        return score;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[，。；、：,.?？!！]", " ");
    }
}

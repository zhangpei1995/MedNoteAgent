package org.med.note.service.impl;

import org.med.note.domain.EvidenceChunk;
import org.med.note.service.spi.RiskAssessor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalRiskAssessor implements RiskAssessor {

    @Override
    public String assess(String question, List<EvidenceChunk> evidence) {
        String text = ((question == null ? "" : question) + " " + evidence).toLowerCase();
        if (containsAny(text, "禁忌", "过敏", "孕妇", "儿童", "肝肾", "合并", "相互作用", "不良反应")) {
            return "HIGH";
        }
        if (containsAny(text, "用法", "用量", "注意", "老人", "症状持续", "加重")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

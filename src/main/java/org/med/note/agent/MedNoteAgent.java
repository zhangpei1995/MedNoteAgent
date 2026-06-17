package org.med.note.agent;

import cn.hutool.core.exceptions.ValidateException;
import org.med.note.domain.EvidenceChunk;
import org.med.note.dto.AgentRequest;
import org.med.note.dto.AgentResponse;
import org.med.note.dto.AgentStep;
import org.med.note.service.spi.AnswerGenerator;
import org.med.note.service.spi.EvidenceRetriever;
import org.med.note.service.spi.RequestPlanner;
import org.med.note.service.spi.RiskAssessor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MedNoteAgent {

    private static final int EVIDENCE_LIMIT = 4;

    private final RequestPlanner requestPlanner;
    private final EvidenceRetriever evidenceRetriever;
    private final RiskAssessor riskAssessor;
    private final AnswerGenerator answerGenerator;

    public MedNoteAgent(
            RequestPlanner requestPlanner,
            EvidenceRetriever evidenceRetriever,
            RiskAssessor riskAssessor,
            AnswerGenerator answerGenerator
    ) {
        this.requestPlanner = requestPlanner;
        this.evidenceRetriever = evidenceRetriever;
        this.riskAssessor = riskAssessor;
        this.answerGenerator = answerGenerator;
    }

    public AgentResponse run(AgentRequest request) {
        AgentRequest safeRequest = request == null ? AgentRequest.empty() : request;
        String input = normalizeInput(safeRequest.input());
        String topic = normalizeTopic(safeRequest.topic(), input);

        RequestPlanner.Plan plan = requestPlanner.plan(topic, input);
        List<EvidenceChunk> evidence = evidenceRetriever.search(topic, plan.rewrittenQuery(), plan.queryKeywords(), EVIDENCE_LIMIT);
        String riskLevel = riskAssessor.assess(input, evidence);
        String answer = answerGenerator.generate(topic, input, riskLevel, evidence);
        List<AgentStep> steps = List.of(
                new AgentStep(1, "plan", summarizePlan(plan)),
                new AgentStep(2, "retrieve", summarizeEvidence(evidence)),
                new AgentStep(3, "assess", "风险等级：" + riskLevel),
                new AgentStep(4, "answer", answer)
        );

        return new AgentResponse(
                answer,
                steps,
                Instant.now()
        );
    }

    private String normalizeTopic(String topic, String input) {
        if (topic != null && !topic.isBlank()) {
            return topic.trim();
        }
        return input.length() > 32 ? input.substring(0, 32) : input;
    }

    private String normalizeInput(String input) {
        if (input == null || input.isBlank()) {
            throw new ValidateException("input 为空");
        }
        return input.trim();
    }

    private String summarizePlan(RequestPlanner.Plan plan) {
        return "意图：" + plan.intent()
                + "；查询目标：" + String.join("、", plan.queryTargets())
                + "；推荐说明书：" + String.join("、", plan.recommendedInstructions());
    }

    private String summarizeEvidence(List<EvidenceChunk> evidence) {
        if (evidence.isEmpty()) {
            return "未命中说明书证据";
        }
        return "命中证据：" + evidence.stream()
                .map(chunk -> chunk.id() + "/" + chunk.drugName() + "/" + chunk.section())
                .reduce((left, right) -> left + "、" + right)
                .orElse("");
    }
}

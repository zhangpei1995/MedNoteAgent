package org.med.note.agent;

import org.med.note.domain.EvidenceChunk;
import org.med.note.dto.AgentRunRequest;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.AgentStep;
import org.med.note.dto.EvidenceReference;
import org.med.note.service.MedicalAnswerGenerator;
import org.med.note.service.MedicalRiskAssessor;
import org.med.note.service.MockDrugKnowledgeBase;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MedNoteAgent {

    private final MockDrugKnowledgeBase knowledgeBase;
    private final MedicalRiskAssessor riskAssessor;
    private final MedicalAnswerGenerator answerGenerator;

    public MedNoteAgent(
            MockDrugKnowledgeBase knowledgeBase,
            MedicalRiskAssessor riskAssessor,
            MedicalAnswerGenerator answerGenerator
    ) {
        this.knowledgeBase = knowledgeBase;
        this.riskAssessor = riskAssessor;
        this.answerGenerator = answerGenerator;
    }

    public AgentRunResponse run(AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        AgentExecution execution = execute(safeRequest);
        String summary = "Demo agent completed " + execution.steps().size()
                + " steps for topic: " + execution.topic()
                + ", risk: " + execution.riskLevel()
                + ", evidence: " + execution.evidence().size() + ".";
        return new AgentRunResponse(
                "med-note-demo-agent",
                summary,
                execution.finalAnswer(),
                execution.riskLevel(),
                execution.evidence().stream().map(this::toReference).toList(),
                execution.steps(),
                Instant.now()
        );
    }

    public List<AgentStep> buildDemoSteps(AgentRunRequest request) {
        return execute(request == null ? AgentRunRequest.empty() : request).steps();
    }

    private AgentExecution execute(AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        String topic = normalizeTopic(safeRequest.topic());
        String input = normalizeInput(safeRequest.input());
        String intent = recognizeIntent(topic, input);
        String rewrittenQuery = rewriteQuery(topic, input, intent);
        List<EvidenceChunk> evidence = knowledgeBase.search(topic, rewrittenQuery, 4);
        String riskLevel = riskAssessor.assess(input, evidence);
        String finalAnswer = answerGenerator.generate(topic, input, riskLevel, evidence);

        List<AgentStep> steps = new ArrayList<>();
        steps.add(new AgentStep(1, "intent", "识别任务意图: " + intent + "；主题: " + topic));
        steps.add(new AgentStep(2, "rewrite", "生成检索 query: " + rewrittenQuery));
        steps.add(new AgentStep(3, "retrieve", formatEvidenceStep(evidence)));
        steps.add(new AgentStep(4, "risk", "医学安全风险等级: " + riskLevel));
        steps.add(new AgentStep(5, "prompt", answerGenerator.buildUserPrompt(topic, input, riskLevel, evidence)));
        steps.add(new AgentStep(6, "final", finalAnswer));

        return new AgentExecution(topic, input, intent, rewrittenQuery, evidence, riskLevel, finalAnswer, steps);
    }

    private String recognizeIntent(String topic, String input) {
        String text = topic + " " + input;
        if (containsAny(text, "禁忌", "过敏")) {
            return "CONTRAINDICATION";
        }
        if (containsAny(text, "不良反应", "副作用")) {
            return "ADVERSE_REACTION";
        }
        if (containsAny(text, "用法", "用量", "怎么吃", "服用")) {
            return "DOSAGE_ADVICE";
        }
        if (containsAny(text, "孕妇", "儿童", "老人", "肝肾")) {
            return "SPECIAL_POPULATION";
        }
        if (containsAny(text, "注意事项", "注意")) {
            return "CAUTION";
        }
        return "GENERAL_QA";
    }

    private String rewriteQuery(String topic, String input, String intent) {
        return (topic + " " + input + " " + switch (intent) {
            case "CONTRAINDICATION" -> "禁忌 过敏 慎用";
            case "ADVERSE_REACTION" -> "不良反应 副作用 停药";
            case "DOSAGE_ADVICE" -> "用法用量 开水冲服 一日";
            case "SPECIAL_POPULATION" -> "儿童 孕妇 老人 肝肾功能";
            case "CAUTION" -> "注意事项 忌口 就医";
            default -> "功能主治 注意事项 用法用量";
        }).trim();
    }

    private String formatEvidenceStep(List<EvidenceChunk> evidence) {
        if (evidence.isEmpty()) {
            return "mock 知识库未命中证据";
        }
        return "mock 知识库命中 " + evidence.size() + " 条证据: "
                + evidence.stream()
                .map(chunk -> chunk.id() + "(" + chunk.drugName() + "/" + chunk.section() + ", score=" + chunk.score() + ")")
                .toList();
    }

    private EvidenceReference toReference(EvidenceChunk chunk) {
        return new EvidenceReference(chunk.id(), chunk.drugName(), chunk.section(), chunk.content(), chunk.score());
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "药品说明书结构化摘要";
        }
        return topic.trim();
    }

    private String normalizeInput(String input) {
        if (input == null || input.isBlank()) {
            return "未提供原始内容，使用本地示例输入";
        }
        return input.trim();
    }

    private record AgentExecution(
            String topic,
            String input,
            String intent,
            String rewrittenQuery,
            List<EvidenceChunk> evidence,
            String riskLevel,
            String finalAnswer,
            List<AgentStep> steps
    ) {
    }
}

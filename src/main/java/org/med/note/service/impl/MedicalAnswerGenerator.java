package org.med.note.service.impl;

import org.med.note.client.QianwenClient;
import org.med.note.domain.EvidenceChunk;
import org.med.note.service.spi.AnswerGenerator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalAnswerGenerator implements AnswerGenerator {

    private final QianwenClient qianwenClient;

    public MedicalAnswerGenerator(QianwenClient qianwenClient) {
        this.qianwenClient = qianwenClient;
    }

    @Override
    public String generate(String topic, String question, String riskLevel, List<EvidenceChunk> evidence) {
        String prompt = buildUserPrompt(topic, question, riskLevel, evidence);
        String systemPrompt = """
                你是 MedNoteAgent，一个药品说明书问答 agent。
                你必须只依据给定证据回答；证据不足时明确说明不足。
                回答要包含：结论、依据、风险提示。不要替代医生诊断或处方。
                """;

        try {
            if (qianwenClient.isConfigured()) {
                String answer = qianwenClient.chatWithConfiguredModel(systemPrompt, prompt);
                if (answer != null && !answer.isBlank()) {
                    return answer.trim();
                }
            }
        } catch (Exception ignored) {
            // Demo should still be runnable without network or a valid API key.
        }
        return generateTemplateAnswer(question, riskLevel, evidence);
    }

    public String buildUserPrompt(String topic, String question, String riskLevel, List<EvidenceChunk> evidence) {
        return """
                任务主题：%s
                用户问题：%s
                风险等级：%s

                检索证据：
                %s

                请生成可追溯的中文医学说明书问答。
                """.formatted(topic, question, riskLevel, formatEvidence(evidence));
    }

    private String generateTemplateAnswer(String question, String riskLevel, List<EvidenceChunk> evidence) {
        if (evidence.isEmpty()) {
            return "未在 mock 知识库中检索到足够证据，暂不能给出基于说明书的回答。请补充药品名称或具体章节。";
        }

        String conclusion = evidence.stream()
                .limit(2)
                .map(chunk -> chunk.section() + "：" + chunk.content())
                .collect(Collectors.joining("\n"));

        String warning = switch (riskLevel) {
            case "HIGH" -> "该问题涉及禁忌、特殊人群、过敏或不良反应，建议由医生或药师结合个体情况判断。";
            case "MEDIUM" -> "该问题涉及用法用量或注意事项，请按说明书和医嘱使用，症状持续或加重时及时就医。";
            default -> "以上仅为基于说明书证据的辅助信息，不能替代专业诊疗建议。";
        };

        return """
                结论：针对“%s”，mock 知识库提示如下。
                %s

                依据：已引用 %d 条说明书证据，主要来自 %s。

                风险提示：%s
                """.formatted(
                question,
                conclusion,
                evidence.size(),
                evidence.stream().map(chunk -> chunk.drugName() + "/" + chunk.section()).distinct().collect(Collectors.joining("、")),
                warning
        ).trim();
    }

    private String formatEvidence(List<EvidenceChunk> evidence) {
        if (evidence.isEmpty()) {
            return "无";
        }
        return evidence.stream()
                .map(chunk -> "- [%s] %s/%s：%s".formatted(chunk.id(), chunk.drugName(), chunk.section(), chunk.content()))
                .collect(Collectors.joining("\n"));
    }
}

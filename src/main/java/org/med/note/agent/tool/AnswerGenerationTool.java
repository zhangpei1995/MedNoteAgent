package org.med.note.agent.tool;

import org.med.note.agent.answer.AnswerGenerationContext;
import org.med.note.agent.answer.AnswerGenerator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AgentToolDefinition(
        name = "answer_generation",
        description = "基于上下文证据生成最终回答。当前用于本地规则链路，后续可替换为完整 LLM generation pipeline。",
        phase = "generation",
        order = 50,
        required = true,
        dependsOn = {"drug_knowledge_search", "medical_risk_assessment"},
        parallelizable = false,
        keywordHints = {"回答生成", "证据总结", "结论", "风险提示", "LLM生成"},
        triggers = {"回答", "摘要", "生成", "问答", "结论", "GENERAL_QA", "DOSAGE_ADVICE", "CAUTION", "CONTRAINDICATION", "ADVERSE_REACTION", "SPECIAL_POPULATION"}
)
public class AnswerGenerationTool implements AgentTool {

    private final AnswerGenerator answerGenerator;

    public AnswerGenerationTool(AnswerGenerator answerGenerator) {
        this.answerGenerator = answerGenerator;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String answer = answerGenerator.generate(new AnswerGenerationContext(
                context.topic(),
                context.input(),
                context.riskLevel(),
                context.evidence()
        ));
        return ToolResult.of(
                "answer_generation",
                "已生成最终回答",
                context.topic(),
                context.taskKeywords(),
                context.intent(),
                context.rewrittenQuery(),
                context.queryKeywords(),
                context.evidence(),
                context.riskLevel(),
                answer,
                answer,
                Map.of("answerLength", answer.length(), "mode", "template-answer-generator")
        );
    }
}

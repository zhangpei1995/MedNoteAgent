package org.med.note.agent.tool;

import org.med.note.service.spi.RiskAssessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AgentToolDefinition(
        name = "medical_risk_assessment",
        description = "根据用户问题和证据识别医学安全风险等级。当前为 demo 规则，后续可替换为安全策略工具。",
        phase = "safety",
        order = 40,
        required = true,
        keywordHints = {"风险评估", "禁忌判断", "特殊人群", "过敏风险", "安全策略"},
        triggers = {"风险", "禁忌", "过敏", "孕妇", "儿童", "老人", "肝肾", "合并", "注意", "不良反应", "CONTRAINDICATION", "SPECIAL_POPULATION", "CAUTION", "ADVERSE_REACTION"}
)
public class MedicalRiskAssessmentTool implements AgentTool {

    private final RiskAssessor riskAssessor;

    public MedicalRiskAssessmentTool(RiskAssessor riskAssessor) {
        this.riskAssessor = riskAssessor;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String riskLevel = riskAssessor.assess(context.input(), context.evidence());
        return ToolResult.of(
                "medical_risk_assessment",
                "医学安全风险等级: " + riskLevel,
                context.taskKeywords(),
                context.intent(),
                context.rewrittenQuery(),
                context.queryKeywords(),
                context.evidence(),
                riskLevel,
                context.finalAnswer(),
                "医学安全风险等级: " + riskLevel,
                Map.of("riskLevel", riskLevel, "mode", "demo-rule-placeholder")
        );
    }
}

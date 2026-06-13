package org.med.note.agent.tool;

import org.med.note.agent.planning.RequestPlanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consolidates strongly-coupled request understanding steps into one planning tool.
 */
@Component
@AgentToolDefinition(
        name = "request_planning",
        description = "一次性完成关键词、意图、查询目标、query 改写、用药风险初判和说明书推荐。",
        phase = "planning",
        order = 10,
        required = true,
        keywordHints = {"请求理解", "意图识别", "关键词", "query改写", "说明书推荐"},
        triggers = {"关键词", "意图", "检索", "禁忌", "过敏", "不良反应", "用法", "用量", "注意", "说明书"}
)
public class RequestPlanningTool implements AgentTool {

    private final RequestPlanner requestPlanner;
    private final String keywordModel;
    private final String intentModel;

    public RequestPlanningTool(
            RequestPlanner requestPlanner,
            @Value("${mednote.agent.keyword.small-model.model:qwen-turbo}") String keywordModel,
            @Value("${mednote.agent.intent.small-model.model:qwen-turbo}") String intentModel
    ) {
        this.requestPlanner = requestPlanner;
        this.keywordModel = keywordModel;
        this.intentModel = intentModel;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        RequestPlanner.Plan plan = requestPlanner.plan(context.input());
        return ToolResult.of(
                "request_planning",
                "完成请求理解: intent=" + plan.intent()
                        + "；topic=" + plan.topic()
                        + "；queryTargets=" + plan.queryTargets()
                        + "；risk=" + plan.medicationRiskLevel()
                        + "；recommended=" + plan.recommendedInstructions(),
                plan.topic(),
                plan.taskKeywords(),
                plan.intent(),
                plan.rewrittenQuery(),
                plan.queryKeywords(),
                context.evidence(),
                plan.medicationRiskLevel(),
                context.finalAnswer(),
                plan.rewrittenQuery(),
                Map.of(
                        "topic", plan.topic(),
                        "taskKeywords", plan.taskKeywords(),
                        "queryKeywords", plan.queryKeywords(),
                        "queryTargets", plan.queryTargets(),
                        "medicationRiskLevel", plan.medicationRiskLevel(),
                        "medicationRiskSignals", plan.medicationRiskSignals(),
                        "recommendedInstructions", plan.recommendedInstructions(),
                        "keywordModel", keywordModel,
                        "intentModel", intentModel,
                        "mode", "rule-based"
                )
        );
    }
}

package org.med.note.agent.tool;

import org.med.note.agent.retrieval.EvidenceRetrievalRequest;
import org.med.note.agent.retrieval.EvidenceRetrievalResult;
import org.med.note.agent.retrieval.EvidenceSearchService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AgentToolDefinition(
        name = "drug_knowledge_search",
        description = "检索已处理的药品说明书片段，返回可追溯证据。当前接入 fixture 知识库，后续可替换为向量检索或图谱检索。",
        phase = "retrieval",
        order = 30,
        required = true,
        dependsOn = {"request_planning"},
        keywordHints = {"说明书检索", "证据召回", "药品知识库", "章节召回", "知识片段"},
        triggers = {"功能主治", "适应症", "用法", "用量", "注意", "禁忌", "不良反应", "证据", "检索", "GENERAL_QA", "DOSAGE_ADVICE", "CAUTION", "CONTRAINDICATION", "ADVERSE_REACTION", "SPECIAL_POPULATION"}
)
public class DrugKnowledgeSearchTool implements AgentTool {

    private final EvidenceSearchService evidenceSearchService;

    public DrugKnowledgeSearchTool(EvidenceSearchService evidenceSearchService) {
        this.evidenceSearchService = evidenceSearchService;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        EvidenceRetrievalResult retrievalResult = evidenceSearchService.retrieve(new EvidenceRetrievalRequest(
                context.topic(),
                context.rewrittenQuery(),
                context.queryKeywords(),
                context.intent(),
                context.riskLevel(),
                4
        ));
        String summary = retrievalResult.evidence().isEmpty()
                ? "未命中药品说明书证据"
                : "以 " + retrievalResult.mode() + " 模式命中 " + retrievalResult.evidence().size() + " 条药品说明书证据";
        return ToolResult.of(
                "drug_knowledge_search",
                summary,
                context.topic(),
                context.taskKeywords(),
                context.intent(),
                context.rewrittenQuery(),
                context.queryKeywords(),
                retrievalResult.evidence(),
                context.riskLevel(),
                context.finalAnswer(),
                summary,
                Map.of(
                        "query", context.rewrittenQuery(),
                        "queryKeywords", context.queryKeywords(),
                        "mode", retrievalResult.mode().name(),
                        "retrieval", retrievalResult.metadata()
                )
        );
    }
}

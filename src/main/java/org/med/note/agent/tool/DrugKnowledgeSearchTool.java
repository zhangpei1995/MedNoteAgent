package org.med.note.agent.tool;

import org.med.note.domain.EvidenceChunk;
import org.med.note.service.spi.EvidenceRetriever;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AgentToolDefinition(
        name = "drug_knowledge_search",
        description = "检索已处理的药品说明书片段，返回可追溯证据。当前接入 mock 知识库，后续可替换为向量检索或图谱检索。",
        phase = "retrieval",
        order = 30,
        required = true,
        keywordHints = {"说明书检索", "证据召回", "药品知识库", "章节召回", "知识片段"},
        triggers = {"功能主治", "适应症", "用法", "用量", "注意", "禁忌", "不良反应", "证据", "检索", "GENERAL_QA", "DOSAGE_ADVICE", "CAUTION", "CONTRAINDICATION", "ADVERSE_REACTION", "SPECIAL_POPULATION"}
)
public class DrugKnowledgeSearchTool implements AgentTool {

    private final EvidenceRetriever evidenceRetriever;

    public DrugKnowledgeSearchTool(EvidenceRetriever evidenceRetriever) {
        this.evidenceRetriever = evidenceRetriever;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        List<EvidenceChunk> evidence = evidenceRetriever.search(context.topic(), context.rewrittenQuery(), context.queryKeywords(), 4);
        String summary = evidence.isEmpty()
                ? "未命中药品说明书证据"
                : "命中 " + evidence.size() + " 条药品说明书证据";
        return ToolResult.of(
                "drug_knowledge_search",
                summary,
                context.taskKeywords(),
                context.intent(),
                context.rewrittenQuery(),
                context.queryKeywords(),
                evidence,
                context.riskLevel(),
                context.finalAnswer(),
                summary,
                Map.of("query", context.rewrittenQuery(), "queryKeywords", context.queryKeywords(), "mode", "mock-retrieval-placeholder")
        );
    }
}

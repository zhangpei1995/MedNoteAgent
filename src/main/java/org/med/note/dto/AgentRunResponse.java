package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AgentRunResponse(
        @Schema(description = "Agent 名称", example = "med-note-demo-agent")
        String agentName,

        @Schema(description = "执行摘要")
        String summary,

        @Schema(description = "最终回答")
        String finalAnswer,

        @Schema(description = "医学风险等级", example = "MEDIUM")
        String riskLevel,

        @Schema(description = "引用证据")
        List<EvidenceReference> evidence,

        @Schema(description = "Agent 执行步骤")
        List<AgentStep> steps,

        @Schema(description = "完成时间")
        Instant finishedAt
) {
}

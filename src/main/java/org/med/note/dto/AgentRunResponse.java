package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AgentRunResponse(
        @Schema(description = "Agent 名称", example = "med-note-demo-agent")
        String agentName,

        @Schema(description = "执行摘要")
        String summary,

        @Schema(description = "Agent 执行步骤")
        List<AgentStep> steps,

        @Schema(description = "完成时间")
        Instant finishedAt
) {
}

package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AgentResponse(
        @Schema(description = "Agent 输出")
        String output,

        @Schema(description = "Agent 执行步骤")
        List<AgentStep> steps,

        @Schema(description = "完成时间")
        Instant finishedAt
) {
}

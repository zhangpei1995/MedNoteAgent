package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentRequest(
        @Schema(description = "Agent 输入", example = "请处理这段文本。")
        String input
) {
    public static AgentRequest empty() {
        return new AgentRequest(null);
    }
}

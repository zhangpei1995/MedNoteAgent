package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentRequest(
        @Schema(description = "任务主题", example = "菖麻熄风颗粒用药安全")
        String topic,

        @Schema(description = "用户问题", example = "过敏体质能不能服用？")
        String input
) {
    public AgentRequest(String input) {
        this(null, input);
    }

    public static AgentRequest empty() {
        return new AgentRequest(null, null);
    }
}

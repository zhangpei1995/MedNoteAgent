package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentRunRequest(
        @Schema(description = "本次 demo agent 的任务主题", example = "二冬汤颗粒说明书摘要")
        String topic,

        @Schema(description = "用于 agent 测试的输入文本", example = "请抽取适应症、用法用量、不良反应和注意事项。")
        String input
) {
    public static AgentRunRequest empty() {
        return new AgentRunRequest(null, null);
    }
}

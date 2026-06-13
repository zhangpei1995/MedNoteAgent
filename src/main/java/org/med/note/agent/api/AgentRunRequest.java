package org.med.note.agent.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

public record AgentRunRequest(
        @JsonAlias("input")
        @Schema(description = "用户输入的自然语言问题", example = "对菖麻熄风颗粒成分过敏的人能不能服用？")
        String question
) {
    public static AgentRunRequest empty() {
        return new AgentRunRequest(null);
    }
}

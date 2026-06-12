package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record AgentStep(
        @Schema(description = "步骤序号", example = "1")
        int order,

        @Schema(description = "步骤名称", example = "extract")
        String stage,

        @Schema(description = "步骤输出内容", example = "读取输入并抽取候选医学信息")
        String content,

        @Schema(description = "面向对话 UI 的事件类型", example = "tool")
        String eventType,

        @Schema(description = "执行状态", example = "completed")
        String status,

        @Schema(description = "扩展元数据")
        Map<String, Object> metadata,

        @Schema(description = "事件时间")
        Instant createdAt
) {
    public AgentStep(int order, String stage, String content) {
        this(order, stage, content, "message", "completed", Map.of(), Instant.now());
    }

    public static AgentStep thought(int order, String stage, String content, Map<String, Object> metadata) {
        return new AgentStep(order, stage, content, "thought", "completed", metadata == null ? Map.of() : metadata, Instant.now());
    }

    public static AgentStep tool(int order, String stage, String content, Map<String, Object> metadata) {
        return new AgentStep(order, stage, content, "tool", "completed", metadata == null ? Map.of() : metadata, Instant.now());
    }

    public static AgentStep message(int order, String stage, String content, Map<String, Object> metadata) {
        return new AgentStep(order, stage, content, "message", "completed", metadata == null ? Map.of() : metadata, Instant.now());
    }
}

package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AgentStep(
        @Schema(description = "步骤序号", example = "1")
        int order,

        @Schema(description = "步骤名称", example = "extract")
        String stage,

        @Schema(description = "步骤输出内容", example = "读取输入并抽取候选医学信息")
        String content
) {
}

package org.med.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EvidenceReference(
        @Schema(description = "证据片段 ID", example = "mock-edt-indication")
        String id,

        @Schema(description = "药品名称", example = "二冬汤颗粒")
        String drugName,

        @Schema(description = "说明书章节", example = "功能主治")
        String section,

        @Schema(description = "证据内容")
        String content,

        @Schema(description = "检索相关度分数")
        double score
) {
}

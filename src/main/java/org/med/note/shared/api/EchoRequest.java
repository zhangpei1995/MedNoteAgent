package org.med.note.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EchoRequest(
        @Schema(description = "需要回显的测试文本", example = "hello med note")
        @NotBlank(message = "message 不能为空")
        String message
) {
}

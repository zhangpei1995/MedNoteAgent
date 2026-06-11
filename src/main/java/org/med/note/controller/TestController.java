package org.med.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.med.note.dto.ApiResponse;
import org.med.note.dto.EchoRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Tag(name = "基础测试接口", description = "用于验证 Spring Boot Controller 是否正常工作")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Operation(summary = "健康探测", description = "返回服务状态、应用名称和当前时间")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "service", "MedNoteAgent",
                "status", "UP",
                "time", Instant.now()
        ));
    }

    @Operation(summary = "回显测试", description = "验证 POST JSON 请求和参数校验")
    @PostMapping("/echo")
    public ApiResponse<Map<String, String>> echo(@Valid @RequestBody EchoRequest request) {
        return ApiResponse.ok("echo success", Map.of("message", request.message()));
    }
}

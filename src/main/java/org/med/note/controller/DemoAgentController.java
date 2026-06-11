package org.med.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.med.note.agent.MedNoteAgent;
import org.med.note.dto.AgentRunRequest;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.AgentStep;
import org.med.note.dto.ApiResponse;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Tag(name = "Demo Agent 测试", description = "本地 demo agent 同步和 SSE 流式调试接口")
@RestController
@RequestMapping("/api/demo-agent")
public class DemoAgentController {

    private final MedNoteAgent medNoteAgent;
    private final TaskExecutor taskExecutor;

    public DemoAgentController(MedNoteAgent medNoteAgent, TaskExecutor taskExecutor) {
        this.medNoteAgent = medNoteAgent;
        this.taskExecutor = taskExecutor;
    }

    @Operation(summary = "运行 demo agent", description = "同步返回 demo agent 的结构化执行步骤")
    @PostMapping("/run")
    public ApiResponse<AgentRunResponse> run(@RequestBody(required = false) AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        return ApiResponse.ok("agent run success", medNoteAgent.run(safeRequest));
    }

    @Operation(summary = "SSE 流式运行 demo agent", description = "以 Server-Sent Events 逐步返回 agent 执行过程")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Parameter(description = "任务主题", example = "二冬汤颗粒说明书摘要")
            @RequestParam(required = false) String topic,
            @Parameter(description = "输入文本", example = "请抽取适应症、用法用量、不良反应和注意事项。")
            @RequestParam(required = false) String input
    ) {
        SseEmitter emitter = new SseEmitter(30_000L);
        AgentRunRequest request = new AgentRunRequest(topic, input);
        List<AgentStep> steps = medNoteAgent.buildDemoSteps(request);

        taskExecutor.execute(() -> emitAgentSteps(emitter, steps));
        return emitter;
    }

    private void emitAgentSteps(SseEmitter emitter, List<AgentStep> steps) {
        try {
            for (AgentStep step : steps) {
                emitter.send(SseEmitter.event()
                        .name("agent-step")
                        .id(String.valueOf(step.order()))
                        .data(step));
                Thread.sleep(400L);
            }
            emitter.send(SseEmitter.event()
                    .name("agent-complete")
                    .data(ApiResponse.ok("stream complete", "done")));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emitter.completeWithError(e);
        }
    }
}

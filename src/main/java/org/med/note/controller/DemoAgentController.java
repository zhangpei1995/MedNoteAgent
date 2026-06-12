package org.med.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.med.note.agent.MedNoteAgent;
import org.med.note.agent.runtime.AgentRunRecord;
import org.med.note.agent.runtime.ToolCallRecord;
import org.med.note.agent.tool.AgentToolDescriptor;
import org.med.note.dto.AgentRunRequest;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.AgentStep;
import org.med.note.dto.ApiResponse;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Tag(name = "Demo Agent 测试", description = "本地 demo agent 同步、任务动态和 SSE 流式调试接口")
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

    @Operation(summary = "查看 agent 任务动态", description = "返回类似 GPT 对话的 thought/tool/message 事件序列")
    @PostMapping("/see")
    public ApiResponse<List<AgentStep>> see(@RequestBody(required = false) AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        return ApiResponse.ok("agent dynamics ready", medNoteAgent.buildDemoSteps(safeRequest));
    }

    @Operation(summary = "查看可接入工具", description = "返回通过注解声明并可由 agent 剪枝选择的工具清单")
    @GetMapping("/tools")
    public ApiResponse<List<AgentToolDescriptor>> tools() {
        return ApiResponse.ok("agent tools ready", medNoteAgent.availableTools());
    }

    @Operation(summary = "查看最近 agent 会话", description = "返回本地内存中的最近会话审计记录")
    @GetMapping("/sessions")
    public ApiResponse<List<AgentRunRecord>> sessions(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok("agent sessions ready", medNoteAgent.recentSessions(limit));
    }

    @Operation(summary = "查看失败工具调用", description = "返回最近失败的工具调用记录，便于排查降级和异常")
    @GetMapping("/tool-call-failures")
    public ApiResponse<List<ToolCallRecord>> toolCallFailures(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok("agent tool failures ready", medNoteAgent.failedToolCalls(limit));
    }

    @Operation(summary = "查看 agent 会话记录", description = "按 sessionId 返回本地内存中的工具调用审计记录")
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AgentRunRecord> session(@PathVariable String sessionId) {
        return ApiResponse.ok("agent session ready", medNoteAgent.findSession(sessionId).orElse(null));
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
                        .name("agent-" + step.eventType())
                        .id(String.valueOf(step.order()))
                        .data(step));
                Thread.sleep(300L);
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

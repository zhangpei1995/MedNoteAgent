package org.med.note.agent;

import org.med.note.agent.runtime.AgentSession;
import org.med.note.agent.runtime.AgentContextMerger;
import org.med.note.agent.runtime.AgentExecutionResult;
import org.med.note.agent.runtime.AgentRunStore;
import org.med.note.agent.runtime.AgentRunRecord;
import org.med.note.agent.runtime.AgentRunResponseFactory;
import org.med.note.agent.runtime.AgentStepFactory;
import org.med.note.agent.runtime.AgentToolExecutor;
import org.med.note.agent.runtime.AgentToolPlanner;
import org.med.note.agent.runtime.ToolCallRecord;
import org.med.note.agent.runtime.ToolExecutionResult;
import org.med.note.agent.runtime.ToolSelectionBatch;
import org.med.note.agent.tool.AgentToolDescriptor;
import org.med.note.agent.tool.AgentToolRegistry;
import org.med.note.agent.tool.ToolContext;
import org.med.note.dto.AgentRunRequest;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.AgentStep;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MedNoteAgent {

    private static final int MAX_TOOL_ITERATIONS = 8;

    private final AgentToolRegistry toolRegistry;
    private final AgentToolPlanner toolPlanner;
    private final AgentRunStore runStore;
    private final AgentToolExecutor toolExecutor;
    private final AgentContextMerger contextMerger;
    private final AgentRunResponseFactory responseFactory;
    private final AgentStepFactory stepFactory;

    public MedNoteAgent(
            AgentToolRegistry toolRegistry,
            AgentToolPlanner toolPlanner,
            AgentRunStore runStore,
            AgentToolExecutor toolExecutor,
            AgentContextMerger contextMerger,
            AgentRunResponseFactory responseFactory,
            AgentStepFactory stepFactory
    ) {
        this.toolRegistry = toolRegistry;
        this.toolPlanner = toolPlanner;
        this.runStore = runStore;
        this.toolExecutor = toolExecutor;
        this.contextMerger = contextMerger;
        this.responseFactory = responseFactory;
        this.stepFactory = stepFactory;
    }

    public AgentRunResponse run(AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        return responseFactory.create(execute(safeRequest));
    }

    public List<AgentStep> buildDemoSteps(AgentRunRequest request) {
        return execute(request == null ? AgentRunRequest.empty() : request).steps();
    }

    public List<AgentToolDescriptor> availableTools() {
        return toolRegistry.listDescriptors();
    }

    public Optional<AgentRunRecord> findSession(String sessionId) {
        return runStore.findBySessionId(sessionId);
    }

    public List<AgentRunRecord> recentSessions(int limit) {
        return runStore.recent(limit);
    }

    public List<ToolCallRecord> failedToolCalls(int limit) {
        return runStore.failedToolCalls(limit);
    }

    private AgentExecutionResult execute(AgentRunRequest request) {
        String topic = normalizeTopic(request.topic());
        String input = normalizeInput(request.input());
        AgentSession session = AgentSession.start();
        Map<String, Object> memory = new HashMap<>();
        memory.put("sessionId", session.id());
        ToolContext context = new ToolContext(topic, input, List.of(), "", "", List.of(), List.of(), "LOW", "", memory);

        List<AgentStep> steps = new ArrayList<>();
        int order = 1;
        for (int iteration = 1; iteration <= MAX_TOOL_ITERATIONS; iteration++) {
            ToolSelectionBatch decision = toolPlanner.selectNextBatch(context, session.executedToolNames());
            steps.add(stepFactory.toolSelection(order++, session.id(), iteration, decision));
            if (!decision.hasSelection()) {
                break;
            }

            List<ToolExecutionResult> executions = toolExecutor.executeBatch(
                    session.id(),
                    session.toolCalls().size() + 1,
                    decision,
                    context
            );
            boolean hasFailure = false;
            for (ToolExecutionResult execution : executions) {
                session.record(execution.record());
                if (execution.succeeded()) {
                    context = contextMerger.merge(context, execution.result());
                    steps.add(stepFactory.toolCompleted(order++, session.id(), execution));
                } else {
                    hasFailure = true;
                    steps.add(stepFactory.toolFailed(order++, session.id(), execution));
                }
            }

            if (hasFailure || executions.stream().anyMatch(execution -> "answer_generation".equals(execution.record().toolName()))) {
                break;
            }
        }

        String finalAnswer = context.finalAnswer() == null || context.finalAnswer().isBlank()
                ? "Demo agent 未生成最终回答，请检查工具选择记录或接入 answer_generation 工具实现。"
                : context.finalAnswer();
        if (!finalAnswer.equals(context.finalAnswer())) {
            context = context.withFinalAnswer(finalAnswer);
        }
        steps.add(stepFactory.finalMessage(order, session.id(), session, context, finalAnswer));
        runStore.save(session, steps, Instant.now());

        return new AgentExecutionResult(session, topic, input, context.intent(), context.rewrittenQuery(), context.evidence(), context.riskLevel(), finalAnswer, steps);
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "药品说明书结构化摘要";
        }
        return topic.trim();
    }

    private String normalizeInput(String input) {
        if (input == null || input.isBlank()) {
            return "未提供原始内容，使用本地示例输入";
        }
        return input.trim();
    }

}

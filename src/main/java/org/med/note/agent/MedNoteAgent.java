package org.med.note.agent;

import org.med.note.agent.runtime.AgentSession;
import org.med.note.agent.runtime.AgentRunRecord;
import org.med.note.agent.runtime.AgentRunStore;
import org.med.note.agent.runtime.AgentToolPlanner;
import org.med.note.agent.runtime.ToolCallRecord;
import org.med.note.agent.runtime.ToolSelectionDecision;
import org.med.note.agent.tool.AgentToolDescriptor;
import org.med.note.agent.tool.AgentToolRegistry;
import org.med.note.agent.tool.ToolContext;
import org.med.note.agent.tool.ToolResult;
import org.med.note.domain.EvidenceChunk;
import org.med.note.dto.AgentRunRequest;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.AgentStep;
import org.med.note.dto.EvidenceReference;
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

    public MedNoteAgent(AgentToolRegistry toolRegistry, AgentToolPlanner toolPlanner, AgentRunStore runStore) {
        this.toolRegistry = toolRegistry;
        this.toolPlanner = toolPlanner;
        this.runStore = runStore;
    }

    public AgentRunResponse run(AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        AgentExecution execution = execute(safeRequest);
        String summary = "Demo agent session " + execution.session().id()
                + " completed " + execution.session().toolCalls().size()
                + " tool calls and " + execution.steps().size()
                + " dynamic events for topic: " + execution.topic()
                + ", intent: " + execution.intent()
                + ", risk: " + execution.riskLevel()
                + ", evidence: " + execution.evidence().size() + ".";
        return new AgentRunResponse(
                "med-note-demo-agent",
                summary,
                execution.finalAnswer(),
                execution.riskLevel(),
                execution.evidence().stream().map(this::toReference).toList(),
                execution.steps(),
                Instant.now()
        );
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

    private AgentExecution execute(AgentRunRequest request) {
        String topic = normalizeTopic(request.topic());
        String input = normalizeInput(request.input());
        AgentSession session = AgentSession.start();
        Map<String, Object> memory = new HashMap<>();
        memory.put("sessionId", session.id());
        ToolContext context = new ToolContext(topic, input, List.of(), "", "", List.of(), List.of(), "LOW", "", memory);

        List<AgentStep> steps = new ArrayList<>();
        int order = 1;
        for (int iteration = 1; iteration <= MAX_TOOL_ITERATIONS; iteration++) {
            ToolSelectionDecision decision = toolPlanner.selectNext(context, session.executedToolNames());
            steps.add(AgentStep.thought(order++, "tool_selection", decision.reason(), Map.of(
                    "sessionId", session.id(),
                    "iteration", iteration,
                    "selectedTool", decision.hasSelection() ? decision.selectedDescriptor().name() : "",
                    "candidateTools", decision.candidateTools(),
                    "unloadedTools", decision.unloadedTools(),
                    "skippedTools", decision.skippedTools(),
                    "stopReason", decision.stopReason(),
                    "confidence", decision.confidence(),
                    "requiresHumanReview", decision.requiresHumanReview()
            )));
            if (!decision.hasSelection()) {
                break;
            }

            Instant startedAt = Instant.now();
            Map<String, Object> inputSnapshot = inputSnapshot(context);
            try {
                ToolResult result = decision.selectedTool().execute(context);
                Instant finishedAt = Instant.now();
                context = merge(context, result);
                ToolCallRecord record = ToolCallRecord.completed(
                        session.id(),
                        session.toolCalls().size() + 1,
                        decision.selectedDescriptor().name(),
                        decision.selectedDescriptor().phase(),
                        startedAt,
                        finishedAt,
                        result.summary(),
                        inputSnapshot,
                        result.metadata()
                );
                session.record(record);
                steps.add(AgentStep.tool(order++, decision.selectedDescriptor().name(), result.summary(), Map.of(
                        "sessionId", session.id(),
                        "toolCall", record,
                        "result", result.metadata()
                )));

                if ("answer_generation".equals(decision.selectedDescriptor().name())) {
                    break;
                }
            } catch (Exception error) {
                Instant finishedAt = Instant.now();
                ToolCallRecord record = ToolCallRecord.failed(
                        session.id(),
                        session.toolCalls().size() + 1,
                        decision.selectedDescriptor().name(),
                        decision.selectedDescriptor().phase(),
                        startedAt,
                        finishedAt,
                        inputSnapshot,
                        error
                );
                session.record(record);
                steps.add(new AgentStep(
                        order++,
                        decision.selectedDescriptor().name(),
                        record.summary(),
                        "tool",
                        "failed",
                        Map.of("sessionId", session.id(), "toolCall", record),
                        Instant.now()
                ));
                break;
            }
        }

        String finalAnswer = context.finalAnswer() == null || context.finalAnswer().isBlank()
                ? "Demo agent 未生成最终回答，请检查工具选择记录或接入 answer_generation 工具实现。"
                : context.finalAnswer();
        if (!finalAnswer.equals(context.finalAnswer())) {
            context = context.withFinalAnswer(finalAnswer);
        }
        steps.add(AgentStep.message(order, "final", finalAnswer, Map.of(
                "sessionId", session.id(),
                "toolCallCount", session.toolCalls().size(),
                "toolCalls", session.toolCalls(),
                "intent", context.intent(),
                "riskLevel", context.riskLevel(),
                "evidenceCount", context.evidence().size()
        )));
        runStore.save(session, steps, Instant.now());

        return new AgentExecution(session, topic, input, context.intent(), context.rewrittenQuery(), context.evidence(), context.riskLevel(), finalAnswer, steps);
    }

    private ToolContext merge(ToolContext context, ToolResult result) {
        List<String> taskKeywords = result.taskKeywords().isEmpty() ? context.taskKeywords() : result.taskKeywords();
        String intent = result.intent() == null || result.intent().isBlank() ? context.intent() : result.intent();
        String rewrittenQuery = result.rewrittenQuery() == null || result.rewrittenQuery().isBlank() ? context.rewrittenQuery() : result.rewrittenQuery();
        List<String> queryKeywords = result.queryKeywords().isEmpty() ? context.queryKeywords() : result.queryKeywords();
        List<EvidenceChunk> evidence = result.evidence().isEmpty() ? context.evidence() : result.evidence();
        String riskLevel = result.riskLevel() == null || result.riskLevel().isBlank() ? context.riskLevel() : result.riskLevel();
        String finalAnswer = result.finalAnswer() == null || result.finalAnswer().isBlank() ? context.finalAnswer() : result.finalAnswer();
        context.memory().put(result.toolName(), result.metadata());
        return new ToolContext(context.topic(), context.input(), taskKeywords, intent, rewrittenQuery, queryKeywords, evidence, riskLevel, finalAnswer, context.memory());
    }

    private Map<String, Object> inputSnapshot(ToolContext context) {
        return Map.of(
                "intent", context.intent(),
                "taskKeywords", context.taskKeywords(),
                "queryKeywords", context.queryKeywords(),
                "evidenceCount", context.evidence().size(),
                "riskLevel", context.riskLevel()
        );
    }

    private EvidenceReference toReference(EvidenceChunk chunk) {
        return new EvidenceReference(chunk.id(), chunk.drugName(), chunk.section(), chunk.content(), chunk.score());
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

    private record AgentExecution(
            AgentSession session,
            String topic,
            String input,
            String intent,
            String rewrittenQuery,
            List<EvidenceChunk> evidence,
            String riskLevel,
            String finalAnswer,
            List<AgentStep> steps
    ) {
    }
}

package org.med.note.agent.runtime;

import org.med.note.agent.tool.ToolContext;
import org.med.note.dto.AgentStep;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Centralizes user-facing agent event construction.
 */
@Component
public class AgentStepFactory {

    public AgentStep toolSelection(int order, String sessionId, int iteration, ToolSelectionBatch decision) {
        return AgentStep.thought(order, "tool_selection", decision.reason(), Map.ofEntries(
                Map.entry("sessionId", sessionId),
                Map.entry("iteration", iteration),
                Map.entry("selectedTool", decision.firstSelectedToolName()),
                Map.entry("selectedTools", decision.selectedToolNames()),
                Map.entry("parallel", decision.parallel()),
                Map.entry("candidateTools", decision.candidateTools()),
                Map.entry("unloadedTools", decision.unloadedTools()),
                Map.entry("skippedTools", decision.skippedTools()),
                Map.entry("stopReason", decision.stopReason()),
                Map.entry("confidence", decision.confidence()),
                Map.entry("requiresHumanReview", decision.requiresHumanReview())
        ));
    }

    public AgentStep toolCompleted(int order, String sessionId, ToolExecutionResult execution) {
        return AgentStep.tool(order, execution.record().toolName(), execution.record().summary(), Map.of(
                "sessionId", sessionId,
                "toolCall", execution.record(),
                "result", execution.result().metadata()
        ));
    }

    public AgentStep toolFailed(int order, String sessionId, ToolExecutionResult execution) {
        return new AgentStep(
                order,
                execution.record().toolName(),
                execution.record().summary(),
                "tool",
                "failed",
                Map.of("sessionId", sessionId, "toolCall", execution.record()),
                Instant.now()
        );
    }

    public AgentStep finalMessage(int order, String sessionId, AgentSession session, ToolContext context, String finalAnswer) {
        return AgentStep.message(order, "final", finalAnswer, Map.of(
                "sessionId", sessionId,
                "toolCallCount", session.toolCalls().size(),
                "toolCalls", session.toolCalls(),
                "intent", context.intent(),
                "riskLevel", context.riskLevel(),
                "evidenceCount", context.evidence().size()
        ));
    }
}

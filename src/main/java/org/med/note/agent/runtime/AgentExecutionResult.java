package org.med.note.agent.runtime;

import org.med.note.domain.EvidenceChunk;
import org.med.note.dto.AgentStep;

import java.util.List;

/**
 * Immutable result of one agent orchestration run before it is converted to the public DTO.
 */
public record AgentExecutionResult(
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

package org.med.note.agent.runtime;

import org.med.note.dto.AgentStep;

import java.time.Instant;
import java.util.List;

/**
 * Persistable snapshot of one completed agent run for troubleshooting.
 */
public record AgentRunRecord(
        String sessionId,
        Instant startedAt,
        Instant finishedAt,
        List<ToolCallRecord> toolCalls,
        List<AgentStep> steps
) {
}

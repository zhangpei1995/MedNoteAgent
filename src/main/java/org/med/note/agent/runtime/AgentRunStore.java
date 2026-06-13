package org.med.note.agent.runtime;

import org.med.note.agent.api.AgentStep;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Stores agent run audit data.
 */
public interface AgentRunStore {
    AgentRunRecord save(AgentSession session, List<AgentStep> steps, Instant finishedAt);

    Optional<AgentRunRecord> findBySessionId(String sessionId);

    List<AgentRunRecord> recent(int limit);

    List<ToolCallRecord> failedToolCalls(int limit);
}

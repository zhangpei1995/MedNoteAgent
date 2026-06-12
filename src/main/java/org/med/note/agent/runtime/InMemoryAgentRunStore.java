package org.med.note.agent.runtime;

import org.med.note.dto.AgentStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Non-persistent run store for local demo and troubleshooting endpoints.
 */
@Component
public class InMemoryAgentRunStore implements AgentRunStore {

    private final int maxRecords;
    private final Map<String, AgentRunRecord> records = new LinkedHashMap<>();

    public InMemoryAgentRunStore(@Value("${mednote.agent.session.max-records:100}") int maxRecords) {
        this.maxRecords = Math.max(1, maxRecords);
    }

    @Override
    public synchronized AgentRunRecord save(AgentSession session, List<AgentStep> steps, Instant finishedAt) {
        AgentRunRecord record = new AgentRunRecord(
                session.id(),
                session.startedAt(),
                finishedAt,
                session.toolCalls(),
                List.copyOf(steps)
        );
        records.put(record.sessionId(), record);
        evictOldestIfNeeded();
        return record;
    }

    @Override
    public synchronized Optional<AgentRunRecord> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(records.get(sessionId));
    }

    @Override
    public synchronized List<AgentRunRecord> recent(int limit) {
        int safeLimit = Math.max(1, limit);
        List<AgentRunRecord> snapshot = new ArrayList<>(records.values());
        int fromIndex = Math.max(0, snapshot.size() - safeLimit);
        List<AgentRunRecord> recent = new ArrayList<>(snapshot.subList(fromIndex, snapshot.size()));
        java.util.Collections.reverse(recent);
        return recent;
    }

    @Override
    public synchronized List<ToolCallRecord> failedToolCalls(int limit) {
        int safeLimit = Math.max(1, limit);
        return records.values().stream()
                .flatMap(record -> record.toolCalls().stream())
                .filter(toolCall -> toolCall.status() == ToolExecutionStatus.FAILED)
                .sorted((left, right) -> right.finishedAt().compareTo(left.finishedAt()))
                .limit(safeLimit)
                .toList();
    }

    private void evictOldestIfNeeded() {
        while (records.size() > maxRecords) {
            String oldestSessionId = records.keySet().iterator().next();
            records.remove(oldestSessionId);
        }
    }
}

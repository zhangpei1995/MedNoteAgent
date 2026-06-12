package org.med.note.agent.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Per-run session state used for troubleshooting and tool-call audit.
 */
public class AgentSession {

    private final String id;
    private final Instant startedAt;
    private final List<ToolCallRecord> toolCalls = new ArrayList<>();

    private AgentSession(String id, Instant startedAt) {
        this.id = id;
        this.startedAt = startedAt;
    }

    public static AgentSession start() {
        return new AgentSession(UUID.randomUUID().toString(), Instant.now());
    }

    public String id() {
        return id;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public List<ToolCallRecord> toolCalls() {
        return List.copyOf(toolCalls);
    }

    public Set<String> executedToolNames() {
        return toolCalls.stream().map(ToolCallRecord::toolName).collect(Collectors.toSet());
    }

    public void record(ToolCallRecord record) {
        toolCalls.add(record);
    }
}

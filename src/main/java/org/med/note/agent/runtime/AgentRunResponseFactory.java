package org.med.note.agent.runtime;

import org.med.note.domain.EvidenceChunk;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.EvidenceReference;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Converts internal execution results into the public API response DTO.
 */
@Component
public class AgentRunResponseFactory {

    public AgentRunResponse create(AgentExecutionResult execution) {
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

    private EvidenceReference toReference(EvidenceChunk chunk) {
        return new EvidenceReference(chunk.id(), chunk.drugName(), chunk.section(), chunk.content(), chunk.score());
    }
}

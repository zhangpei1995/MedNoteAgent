package org.med.note.agent.answer;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;

/**
 * Internal context consumed by the answer generation capability.
 */
public record AnswerGenerationContext(
        String topic,
        String question,
        String riskLevel,
        List<EvidenceChunk> evidence
) {
}

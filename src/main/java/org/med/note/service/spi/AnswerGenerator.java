package org.med.note.service.spi;

import org.med.note.domain.EvidenceChunk;

import java.util.List;

/**
 * Generates the final answer from topic, question, risk and evidence.
 */
public interface AnswerGenerator {
    String generate(String topic, String question, String riskLevel, List<EvidenceChunk> evidence);
}

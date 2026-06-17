package org.med.note.service.spi;

import org.med.note.domain.EvidenceChunk;

import java.util.List;

/**
 * Assesses medication safety risk from the user question and current evidence.
 */
public interface RiskAssessor {
    String assess(String question, List<EvidenceChunk> evidence);
}

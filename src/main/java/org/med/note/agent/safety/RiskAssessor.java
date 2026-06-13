package org.med.note.agent.safety;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;

/**
 * Assesses medication safety risk from the user question and current evidence.
 */
public interface RiskAssessor {
    String assess(String question, List<EvidenceChunk> evidence);
}

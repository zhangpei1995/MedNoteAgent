package org.med.note.agent.retrieval;

import org.med.note.knowledge.evidence.EvidenceChunk;

import java.util.List;

/**
 * Retrieves evidence chunks for a planned medical question.
 */
public interface EvidenceRetriever {
    List<EvidenceChunk> search(String topic, String query, List<String> queryKeywords, int limit);
}

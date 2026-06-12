package org.med.note.service.spi;

import org.med.note.domain.EvidenceChunk;

import java.util.List;

/**
 * Retrieves evidence chunks for a planned medical question.
 */
public interface EvidenceRetriever {
    List<EvidenceChunk> search(String topic, String query, List<String> queryKeywords, int limit);
}

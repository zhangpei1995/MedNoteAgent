package org.med.note.knowledge.graph;

import java.util.List;
import java.util.Optional;

public interface KnowledgeGraphReader {
    Optional<KnowledgeGraphNode> findNode(String nodeId);

    List<KnowledgeGraphNode> searchNodes(String keyword, int limit);

    List<KnowledgeGraphEdge> findOutgoingEdges(String sourceNodeId, String edgeType, int limit);

    List<KnowledgeGraphEdge> findIncomingEdges(String targetNodeId, String edgeType, int limit);

    long countNodes();
}

package org.med.note.knowledge.graph;

import java.util.List;

public interface KnowledgeGraphWriter {
    KnowledgeGraphNode upsertNode(KnowledgeGraphNode node);

    KnowledgeGraphEdge upsertEdge(KnowledgeGraphEdge edge);

    void upsertSubgraph(List<KnowledgeGraphNode> nodes, List<KnowledgeGraphEdge> edges);
}

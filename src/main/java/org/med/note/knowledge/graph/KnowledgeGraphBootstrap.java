package org.med.note.knowledge.graph;

import org.med.note.domain.EvidenceChunk;
import org.med.note.service.impl.MockDrugKnowledgeBase;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seeds the local demo graph from mock evidence when the SQLite graph is empty.
 */
@Component
public class KnowledgeGraphBootstrap {

    private final KnowledgeGraphStore graphStore;
    private final MockDrugKnowledgeBase mockDrugKnowledgeBase;

    public KnowledgeGraphBootstrap(KnowledgeGraphStore graphStore, MockDrugKnowledgeBase mockDrugKnowledgeBase) {
        this.graphStore = graphStore;
        this.mockDrugKnowledgeBase = mockDrugKnowledgeBase;
    }

    @PostConstruct
    public void seedDemoGraphIfEmpty() {
        if (graphStore.findNode(nodeId("drug", "菖麻熄风颗粒")).isPresent()) {
            return;
        }
        List<KnowledgeGraphNode> nodes = new ArrayList<>();
        List<KnowledgeGraphEdge> edges = new ArrayList<>();
        for (EvidenceChunk evidence : mockDrugKnowledgeBase.allEvidence()) {
            String drugNodeId = nodeId("drug", evidence.drugName());
            String sectionNodeId = nodeId("section", evidence.drugName() + ":" + evidence.section());
            String evidenceNodeId = nodeId("evidence", evidence.id());

            nodes.add(KnowledgeGraphNode.of(drugNodeId, "DRUG", evidence.drugName(), evidence.drugName(), evidence.id(), Map.of()));
            nodes.add(KnowledgeGraphNode.of(sectionNodeId, "INSTRUCTION_SECTION", evidence.section(), evidence.drugName() + ":" + evidence.section(), evidence.id(), Map.of(
                    "drugName", evidence.drugName()
            )));
            nodes.add(KnowledgeGraphNode.of(evidenceNodeId, "EVIDENCE_CHUNK", evidence.id(), evidence.id(), evidence.id(), Map.of(
                    "drugName", evidence.drugName(),
                    "section", evidence.section(),
                    "content", evidence.content()
            )));

            edges.add(KnowledgeGraphEdge.of(edgeId(drugNodeId, sectionNodeId, "HAS_SECTION"), drugNodeId, sectionNodeId, "HAS_SECTION", 1.0, evidence.id(), Map.of()));
            edges.add(KnowledgeGraphEdge.of(edgeId(sectionNodeId, evidenceNodeId, "HAS_EVIDENCE"), sectionNodeId, evidenceNodeId, "HAS_EVIDENCE", 1.0, evidence.id(), Map.of()));
            edges.add(KnowledgeGraphEdge.of(edgeId(evidenceNodeId, drugNodeId, "EVIDENCE_OF"), evidenceNodeId, drugNodeId, "EVIDENCE_OF", 1.0, evidence.id(), Map.of()));
        }
        graphStore.upsertSubgraph(nodes, edges);
    }

    private String nodeId(String type, String value) {
        return type + ":" + normalize(value);
    }

    private String edgeId(String sourceNodeId, String targetNodeId, String type) {
        return sourceNodeId + "->" + type + "->" + targetNodeId;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}

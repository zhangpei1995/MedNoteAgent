package org.med.note.knowledge;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.med.note.knowledge.graph.KnowledgeGraphEdge;
import org.med.note.knowledge.graph.KnowledgeGraphNode;
import org.med.note.knowledge.graph.KnowledgeGraphStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MedNoteAgentApplication.class)
class KnowledgeGraphStoreTest {

    @Autowired
    private KnowledgeGraphStore graphStore;

    @Test
    void bootstrapShouldSeedDrugSectionEvidenceGraph() {
        List<KnowledgeGraphNode> nodes = graphStore.searchNodes("菖麻熄风颗粒", 10);
        assertFalse(nodes.isEmpty());

        String drugNodeId = nodes.stream()
                .filter(node -> "DRUG".equals(node.type()))
                .map(KnowledgeGraphNode::id)
                .findFirst()
                .orElseThrow();
        List<KnowledgeGraphEdge> sectionEdges = graphStore.findOutgoingEdges(drugNodeId, "HAS_SECTION", 10);
        assertFalse(sectionEdges.isEmpty());
    }

    @Test
    void writerShouldUpsertNodeAndEdge() {
        KnowledgeGraphNode drug = KnowledgeGraphNode.of("drug:test", "DRUG", "测试药品", "测试药品", "test-source", Map.of());
        KnowledgeGraphNode section = KnowledgeGraphNode.of("section:test", "INSTRUCTION_SECTION", "注意事项", "测试药品:注意事项", "test-source", Map.of());
        KnowledgeGraphEdge edge = KnowledgeGraphEdge.of("drug:test->HAS_SECTION->section:test", drug.id(), section.id(), "HAS_SECTION", 1.0, "test-source", Map.of());

        graphStore.upsertSubgraph(List.of(drug, section), List.of(edge));

        assertTrue(graphStore.findNode(drug.id()).isPresent());
        assertFalse(graphStore.findOutgoingEdges(drug.id(), "HAS_SECTION", 10).isEmpty());
    }
}

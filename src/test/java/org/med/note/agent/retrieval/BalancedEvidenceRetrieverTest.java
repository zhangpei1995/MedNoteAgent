package org.med.note.agent.retrieval;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MedNoteAgentApplication.class)
class BalancedEvidenceRetrieverTest {

    @Autowired
    private EvidenceSearchService evidenceSearchService;

    @Test
    void highRiskContraindicationShouldUseAccurateMode() {
        EvidenceRetrievalResult result = evidenceSearchService.retrieve(new EvidenceRetrievalRequest(
                "菖麻熄风颗粒说明书问答",
                "菖麻熄风颗粒 过敏 能不能吃 禁忌 慎用",
                List.of("菖麻熄风颗粒", "过敏", "禁忌"),
                "CONTRAINDICATION",
                "HIGH",
                4
        ));

        assertEquals(EvidenceRetrievalMode.ACCURATE, result.mode());
        assertFalse(result.evidence().isEmpty());
        assertEquals("mock-cmxf-contraindication", result.evidence().get(0).id());
        assertTrue(result.metadata().containsKey("elapsedMillis"));
    }

    @Test
    void naturalLanguageQuestionShouldUseSemanticExpansion() {
        EvidenceRetrievalResult result = evidenceSearchService.retrieve(new EvidenceRetrievalRequest(
                "二冬汤颗粒说明书问答",
                "二冬汤颗粒 服药期间有什么忌口",
                List.of("二冬汤颗粒", "忌口"),
                "CAUTION",
                "MEDIUM",
                4
        ));

        assertTrue(List.of(EvidenceRetrievalMode.BALANCED, EvidenceRetrievalMode.ACCURATE).contains(result.mode()));
        assertTrue(result.evidence().stream().anyMatch(evidence -> "mock-edt-caution".equals(evidence.id())));
        assertTrue(result.metadata().get("channels").toString().contains("semantic-synonym"));
    }
}

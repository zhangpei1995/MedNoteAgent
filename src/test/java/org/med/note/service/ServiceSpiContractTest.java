package org.med.note.service;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.med.note.domain.EvidenceChunk;
import org.med.note.service.spi.AnswerGenerator;
import org.med.note.service.spi.EvidenceRetriever;
import org.med.note.service.spi.RequestPlanner;
import org.med.note.service.spi.RiskAssessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = MedNoteAgentApplication.class,
        properties = "mednote.llm.agentscope.enabled=false"
)
class ServiceSpiContractTest {

    @Autowired
    private RequestPlanner requestPlanner;

    @Autowired
    private EvidenceRetriever evidenceRetriever;

    @Autowired
    private RiskAssessor riskAssessor;

    @Autowired
    private AnswerGenerator answerGenerator;

    @Test
    void serviceSpiImplementationsShouldProvideDemoCapabilities() {
        RequestPlanner.Plan plan = requestPlanner.plan("菖麻熄风颗粒用药安全", "过敏体质能不能服用？");
        assertNotNull(plan.intent());
        assertFalse(plan.queryKeywords().isEmpty());

        List<EvidenceChunk> evidence = evidenceRetriever.search("菖麻熄风颗粒用药安全", plan.rewrittenQuery(), plan.queryKeywords(), 4);
        assertFalse(evidence.isEmpty());

        String riskLevel = riskAssessor.assess("过敏体质能不能服用？", evidence);
        assertNotNull(riskLevel);

        String answer = answerGenerator.generate("菖麻熄风颗粒用药安全", "过敏体质能不能服用？", riskLevel, evidence);
        assertFalse(answer.isBlank());
    }
}

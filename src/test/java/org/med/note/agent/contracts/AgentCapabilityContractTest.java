package org.med.note.agent.contracts;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.med.note.agent.answer.AnswerGenerationContext;
import org.med.note.knowledge.evidence.EvidenceChunk;
import org.med.note.agent.answer.AnswerGenerator;
import org.med.note.agent.retrieval.EvidenceRetriever;
import org.med.note.agent.planning.RequestPlanner;
import org.med.note.agent.safety.RiskAssessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = MedNoteAgentApplication.class)
class AgentCapabilityContractTest {

    @Autowired
    private RequestPlanner requestPlanner;

    @Autowired
    private EvidenceRetriever evidenceRetriever;

    @Autowired
    private RiskAssessor riskAssessor;

    @Autowired
    private AnswerGenerator answerGenerator;

    @Test
    void agentCapabilitiesShouldProvideLocalRuleBasedBehavior() {
        RequestPlanner.Plan plan = requestPlanner.plan("对菖麻熄风颗粒成分过敏的人能不能服用？");
        assertNotNull(plan.intent());
        assertFalse(plan.queryKeywords().isEmpty());

        List<EvidenceChunk> evidence = evidenceRetriever.search(plan.topic(), plan.rewrittenQuery(), plan.queryKeywords(), 4);
        assertFalse(evidence.isEmpty());

        String riskLevel = riskAssessor.assess("对菖麻熄风颗粒成分过敏的人能不能服用？", evidence);
        assertNotNull(riskLevel);

        String answer = answerGenerator.generate(new AnswerGenerationContext(
                plan.topic(),
                "对菖麻熄风颗粒成分过敏的人能不能服用？",
                riskLevel,
                evidence
        ));
        assertFalse(answer.isBlank());
    }
}

package org.med.note.eval;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.med.note.agent.MedNoteAgent;
import org.med.note.dto.AgentRequest;
import org.med.note.dto.AgentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MedNoteAgentApplication.class)
class AgentEvalRunnerTest {

    @Autowired
    private MedNoteAgent agent;

    @Test
    void agentShouldRunMedicalQuestionPipeline() {
        AgentResponse response = agent.run(new AgentRequest(
                "菖麻熄风颗粒用药安全",
                "对菖麻熄风颗粒成分过敏的人能不能服用？请说明禁忌和风险。"
        ));

        assertEquals("plan", response.steps().get(0).stage());
        assertEquals("retrieve", response.steps().get(1).stage());
        assertEquals("assess", response.steps().get(2).stage());
        assertEquals("answer", response.steps().get(3).stage());
        assertFalse(response.output().isBlank());
        assertTrue(response.steps().get(1).content().contains("mock-cmxf-contraindication"));
        assertTrue(response.output().contains("风险提示"));
    }
}

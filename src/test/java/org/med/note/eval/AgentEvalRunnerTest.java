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

@SpringBootTest(classes = MedNoteAgentApplication.class)
class AgentEvalRunnerTest {

    @Autowired
    private MedNoteAgent agent;

    @Test
    void minimalAgentLoopShouldReturnLoopAndFinalSteps() {
        AgentResponse response = agent.run(new AgentRequest(
                "抽取适应症、用法用量和注意事项"
        ));

        assertEquals("loop", response.steps().get(0).stage());
        assertEquals("final", response.steps().get(1).stage());
        assertFalse(response.output().isBlank());
    }
}

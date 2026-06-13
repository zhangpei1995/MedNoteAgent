package org.med.note.shared.api;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MedNoteAgentApplication.class)
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingShouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UP")));
    }

    @Test
    void agentRunShouldReturnSteps() throws Exception {
        mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "二冬汤颗粒说明书的适应症、用法用量和注意事项是什么？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.agentName", is("med-note-agent")))
                .andExpect(jsonPath("$.data.steps[0].stage", is("tool_selection")))
                .andExpect(jsonPath("$.data.steps[0].metadata.selectedTool", is("request_planning")))
                .andExpect(jsonPath("$.data.steps[0].metadata.stopReason", is("continue")))
                .andExpect(jsonPath("$.data.steps[0].metadata.confidence").exists())
                .andExpect(jsonPath("$.data.steps[0].metadata.requiresHumanReview").exists())
                .andExpect(jsonPath("$.data.steps[1].stage", is("request_planning")))
                .andExpect(jsonPath("$.data.steps[1].metadata.result.queryTargets[0]").exists())
                .andExpect(jsonPath("$.data.steps[1].metadata.result.recommendedInstructions[0]").exists())
                .andExpect(jsonPath("$.data.steps[3].stage", is("drug_knowledge_search")))
                .andExpect(jsonPath("$.data.steps[3].eventType", is("tool")));
    }

    @Test
    void agentSeeShouldReturnConversationLikeDynamics() throws Exception {
        mockMvc.perform(post("/api/agent/see")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "对菖麻熄风颗粒成分过敏的人能不能服用？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].eventType", is("thought")))
                .andExpect(jsonPath("$.data[0].stage", is("tool_selection")))
                .andExpect(jsonPath("$.data[0].metadata.selectedTool", is("request_planning")))
                .andExpect(jsonPath("$.data[0].metadata.skippedTools").isArray())
                .andExpect(jsonPath("$.data[1].stage", is("request_planning")))
                .andExpect(jsonPath("$.data[1].metadata.result.medicationRiskLevel", is("HIGH")))
                .andExpect(jsonPath("$.data[1].metadata.toolCall.sessionId").exists());
    }

    @Test
    void agentToolsShouldExposeAnnotatedTools() throws Exception {
        mockMvc.perform(get("/api/agent/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    void agentSessionLookupShouldReturnOkForUnknownSession() throws Exception {
        mockMvc.perform(get("/api/agent/sessions/not-found"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void agentSessionsShouldReturnRecentRuns() throws Exception {
        mockMvc.perform(get("/api/agent/sessions?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void agentToolFailuresShouldReturnFailureList() throws Exception {
        mockMvc.perform(get("/api/agent/tool-call-failures?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray());
    }
}

package org.med.note.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.med.note.agent.MedNoteAgent;
import org.med.note.agent.api.AgentRunRequest;
import org.med.note.agent.api.AgentRunResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lightweight eval runner for the Agent fixture.
 */
@SpringBootTest(classes = MedNoteAgentApplication.class)
class AgentEvalRunnerTest {

    @Autowired
    private MedNoteAgent agent;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void evalFixtureShouldMeetExpectedToolEvidenceRiskAndAnswerSignals() throws IOException {
        JsonNode root = objectMapper.readTree(new ClassPathResource("eval/agent-eval-cases.json").getInputStream());
        JsonNode cases = root.path("cases");
        assertTrue(cases.isArray(), "eval cases must be an array");
        assertTrue(cases.size() > 0, "eval suite should contain at least one case");

        List<String> failures = new ArrayList<>();
        int passedCases = 0;
        for (JsonNode evalCase : cases) {
            EvalResult result = evaluate(evalCase);
            if (result.passed()) {
                passedCases++;
            } else {
                failures.add(result.caseId() + ": " + String.join("; ", result.failures()));
            }
        }

        System.out.printf("MedNoteAgent eval report: cases=%d passed=%d passRate=%.2f%n", cases.size(), passedCases, passedCases * 1.0 / cases.size());
        assertTrue(failures.isEmpty(), "agent eval failed: " + failures);
    }

    private EvalResult evaluate(JsonNode evalCase) {
        String caseId = evalCase.path("id").asText();
        JsonNode request = evalCase.path("request");
        JsonNode expected = evalCase.path("expected");
        AgentRunResponse response = agent.run(new AgentRunRequest(request.path("question").asText()));

        List<String> failures = new ArrayList<>();
        assertContainsAll(failures, "tools", toolStages(response), expected.path("mustSelectTools"));
        assertContainsAll(failures, "evidence", evidenceIds(response), expected.path("mustMentionEvidenceIds"));
        if (!expected.path("riskLevel").asText().equals(response.riskLevel())) {
            failures.add("riskLevel expected=" + expected.path("riskLevel").asText() + " actual=" + response.riskLevel());
        }
        assertContainsAll(failures, "queryTargets", planningMetadata(response, "queryTargets"), expected.path("mustQueryTargets"));
        assertContainsAll(failures, "riskSignals", planningMetadata(response, "medicationRiskSignals"), expected.path("mustRiskSignals"));
        assertContainsAll(failures, "recommendedInstructions", planningMetadata(response, "recommendedInstructions"), expected.path("mustRecommendInstructions"));
        for (JsonNode phrase : expected.path("mustContain")) {
            if (!response.finalAnswer().contains(phrase.asText())) {
                failures.add("finalAnswer missing phrase=" + phrase.asText());
            }
        }
        return new EvalResult(caseId, failures.isEmpty(), failures);
    }

    private List<String> toolStages(AgentRunResponse response) {
        return response.steps().stream()
                .filter(step -> "tool".equals(step.eventType()))
                .map(step -> step.stage())
                .toList();
    }

    private List<String> evidenceIds(AgentRunResponse response) {
        return response.evidence().stream().map(evidence -> evidence.id()).toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> planningMetadata(AgentRunResponse response, String fieldName) {
        return response.steps().stream()
                .filter(step -> "request_planning".equals(step.stage()))
                .findFirst()
                .map(step -> step.metadata().get("result"))
                .filter(result -> result instanceof java.util.Map<?, ?>)
                .map(result -> (java.util.Map<String, Object>) result)
                .map(result -> result.get(fieldName))
                .filter(value -> value instanceof List<?>)
                .map(value -> ((List<?>) value).stream().map(String::valueOf).toList())
                .orElse(List.of());
    }

    private void assertContainsAll(List<String> failures, String metric, List<String> actual, JsonNode expected) {
        Set<String> actualSet = new HashSet<>(actual);
        for (JsonNode expectedValue : expected) {
            if (!actualSet.contains(expectedValue.asText())) {
                failures.add(metric + " missing=" + expectedValue.asText() + " actual=" + actual);
            }
        }
    }

    private record EvalResult(String caseId, boolean passed, List<String> failures) {
    }
}

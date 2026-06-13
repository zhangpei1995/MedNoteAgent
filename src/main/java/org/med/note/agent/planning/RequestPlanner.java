package org.med.note.agent.planning;

import java.util.List;

/**
 * Plans one user request into routing fields consumed by agent tools.
 */
public interface RequestPlanner {
    Plan plan(String question);

    record Plan(
            String topic,
            String intent,
            List<String> taskKeywords,
            String rewrittenQuery,
            List<String> queryKeywords,
            List<String> queryTargets,
            String medicationRiskLevel,
            List<String> medicationRiskSignals,
            List<String> recommendedInstructions
    ) {
    }
}

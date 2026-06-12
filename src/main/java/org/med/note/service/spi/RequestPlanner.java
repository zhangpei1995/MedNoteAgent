package org.med.note.service.spi;

import java.util.List;

/**
 * Plans one user request into routing fields consumed by agent tools.
 */
public interface RequestPlanner {
    Plan plan(String topic, String input);

    record Plan(
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

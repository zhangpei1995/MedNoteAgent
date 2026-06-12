package org.med.note.agent.tool;

import java.util.List;

public record AgentToolDescriptor(
        String name,
        String description,
        String phase,
        int order,
        boolean required,
        List<String> dependsOn,
        boolean parallelizable,
        List<String> keywordHints,
        List<String> triggers
) {
}

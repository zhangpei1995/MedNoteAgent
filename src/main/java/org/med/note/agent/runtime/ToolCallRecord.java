package org.med.note.agent.runtime;

import java.time.Instant;
import java.util.Map;

/**
 * Audit record for one tool invocation in an agent session.
 */
public record ToolCallRecord(
        String sessionId,
        int order,
        String toolName,
        String phase,
        ToolExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        String summary,
        Map<String, Object> inputSnapshot,
        Map<String, Object> outputMetadata,
        ToolFailureType failureType,
        String errorType,
        String errorMessage
) {
    public static ToolCallRecord completed(
            String sessionId,
            int order,
            String toolName,
            String phase,
            Instant startedAt,
            Instant finishedAt,
            String summary,
            Map<String, Object> inputSnapshot,
            Map<String, Object> outputMetadata
    ) {
        return new ToolCallRecord(
                sessionId,
                order,
                toolName,
                phase,
                ToolExecutionStatus.COMPLETED,
                startedAt,
                finishedAt,
                java.time.Duration.between(startedAt, finishedAt).toMillis(),
                summary,
                inputSnapshot,
                outputMetadata == null ? Map.of() : outputMetadata,
                ToolFailureType.NONE,
                "",
                ""
        );
    }

    public static ToolCallRecord failed(
            String sessionId,
            int order,
            String toolName,
            String phase,
            Instant startedAt,
            Instant finishedAt,
            Map<String, Object> inputSnapshot,
            Exception error
    ) {
        return new ToolCallRecord(
                sessionId,
                order,
                toolName,
                phase,
                ToolExecutionStatus.FAILED,
                startedAt,
                finishedAt,
                java.time.Duration.between(startedAt, finishedAt).toMillis(),
                "工具调用失败: " + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()),
                inputSnapshot,
                Map.of(),
                classify(error),
                error.getClass().getSimpleName(),
                error.getMessage() == null ? "" : error.getMessage()
        );
    }

    private static ToolFailureType classify(Exception error) {
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (message.contains("timeout") || message.contains("timed out")) {
            return ToolFailureType.MODEL_TIMEOUT;
        }
        return ToolFailureType.TOOL_EXCEPTION;
    }
}

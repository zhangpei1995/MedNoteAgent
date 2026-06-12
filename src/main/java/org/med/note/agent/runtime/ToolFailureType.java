package org.med.note.agent.runtime;

/**
 * Coarse failure categories used for troubleshooting and future fallback policy.
 */
public enum ToolFailureType {
    NONE,
    TOOL_EXCEPTION,
    MODEL_TIMEOUT,
    EMPTY_RETRIEVAL,
    INSUFFICIENT_EVIDENCE,
    SAFETY_BLOCKED
}

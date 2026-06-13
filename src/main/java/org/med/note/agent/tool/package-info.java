/**
 * Pluggable agent tool contracts and local implementations.
 *
 * <p>Core contracts are {@link org.med.note.agent.tool.AgentTool},
 * {@link org.med.note.agent.tool.ToolContext}, and {@link org.med.note.agent.tool.ToolResult}.
 * Local tools are intentionally lightweight adapters that can be replaced by model,
 * retrieval, or safety-policy implementations without changing the agent orchestrator.</p>
 */
package org.med.note.agent.tool;

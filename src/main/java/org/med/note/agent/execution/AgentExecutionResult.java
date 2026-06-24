package org.med.note.agent.execution;

import lombok.Data;

/**
 * Agent 执行完成后的结构化结果和审计信息。
 */
@Data
public class AgentExecutionResult {

    /**
     * Agent 或模型最终返回给用户的回答内容。
     */
    private String assistantOutput;

    /**
     * 模型提供方，例如 dashscope。
     */
    private String modelProvider;

    /**
     * 本轮实际调用的模型名称。
     */
    private String modelName;

    /**
     * 本轮实际使用的系统提示词。
     */
    private String systemPrompt;

    /**
     * 本轮发给模型或 Agent 的完整请求 JSON。
     */
    private String requestJson;

    /**
     * 本轮模型或 Agent 返回的完整响应 JSON。
     */
    private String responseJson;
}

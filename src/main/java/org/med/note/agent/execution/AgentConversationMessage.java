package org.med.note.agent.execution;

import lombok.Data;

/**
 * Agent 执行时可直接传递给模型的一条对话上下文消息。
 *
 * <p>role 使用模型侧可识别的角色语义，例如 user 或 assistant；content 必须保留原始医学语义，
 * 不在消息对象内做摘要、改写或脱敏。</p>
 */
@Data
public class AgentConversationMessage {

    /**
     * 消息角色，例如 user、assistant。
     */
    private String role;

    /**
     * 消息内容；调用方负责保证为空内容不会进入模型请求。
     */
    private String content;

    public static AgentConversationMessage of(String role, String content) {
        AgentConversationMessage message = new AgentConversationMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}

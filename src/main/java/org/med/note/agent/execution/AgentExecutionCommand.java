package org.med.note.agent.execution;

import lombok.Data;

import java.util.List;

/**
 * Agent 执行一轮对话所需的输入命令。
 */
@Data
public class AgentExecutionCommand {

    /**
     * 对话轮次审计 ID，用于追踪本次执行归属。
     */
    private String turnId;

    /**
     * 当前轮次所属会话 ID，用于追踪上下文加载范围。
     */
    private String sessionId;

    /**
     * 用户本轮原始输入，必须原样传入 Agent 以保留医学问答语义。
     */
    private String userInput;

    /**
     * 当前执行实际传给模型的完整对话消息，按模型阅读顺序排列。
     */
    private List<AgentConversationMessage> conversationMessages;
}

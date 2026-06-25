package org.med.note.agent.execution;

import java.util.List;

/**
 * 加载一轮 Agent 执行所需的对话上下文消息。
 *
 * <p>实现类负责根据当前轮次选择上下文加载策略，例如完整历史、窗口裁剪或摘要记忆。
 * 返回结果应按模型阅读顺序排列，并包含当前轮用户输入。</p>
 */
public interface ConversationMessageLoader {

    /**
     * 加载指定轮次执行时需要传给模型的对话消息。
     *
     * @param turnId 当前轮次审计 ID，必须对应一条已落库的轮次记录
     * @return 按时间正序排列的对话消息；不得返回 null
     */
    List<AgentConversationMessage> loadMessages(String turnId);
}

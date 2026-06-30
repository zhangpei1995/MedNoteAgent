package org.med.note.agent.title;

/**
 * 会话标题生成客户端。
 *
 * <p>业务提交链路只提交已落库的会话和来源轮次，具体异步执行、模型调用和状态写回由实现类负责。</p>
 */
public interface SessionTitleGenerationClient {

    /**
     * 异步生成指定会话标题。
     *
     * @param sessionId 需要生成标题的会话 ID
     * @param sourceTurnId 标题生成依据的首轮对话审计 ID
     */
    void generateAsync(String sessionId, String sourceTurnId);
}

package org.med.note.agent.client;

/**
 * 对话 Agent 子系统对业务入口暴露的统一客户端。
 *
 * <p>调用方只需要提交待执行的轮次 ID，不感知 Agent 的具体实现、生命周期状态流转和审计落库细节。</p>
 */
public interface ChatAgentClient {

    /**
     * 异步执行指定对话轮次的 Agent 流程。
     *
     * @param turnId 已落库的对话轮次审计 ID
     */
    void executeAsync(String turnId);
}

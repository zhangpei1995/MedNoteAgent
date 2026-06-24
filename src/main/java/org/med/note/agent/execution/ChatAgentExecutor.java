package org.med.note.agent.execution;

/**
 * 对话 Agent 执行策略。
 *
 * <p>不同模型、不同 Agent 编排方式或不同供应商实现都应实现该接口，调用方不直接依赖具体实现。</p>
 */
public interface ChatAgentExecutor {

    /**
     * 执行一轮用户输入并返回可审计的 Agent 结果。
     *
     * @param command 本轮 Agent 执行命令
     * @return Agent 输出和模型调用审计信息
     */
    AgentExecutionResult execute(AgentExecutionCommand command);
}

package org.med.note.agent.client;

import org.med.note.agent.execution.AgentExecutionCommand;
import org.med.note.agent.execution.AgentExecutionResult;
import org.med.note.agent.execution.ChatAgentExecutor;
import org.med.note.agent.lifecycle.ChatTurnLifecycleManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring 异步执行器的对话 Agent 客户端实现。
 *
 * <p>该类只编排一次轮次执行过程：进入处理中、调用 Agent 执行策略，并根据结果写回成功或失败状态。</p>
 */
@Component
public class AsyncChatAgentClient implements ChatAgentClient {

    private final ChatTurnLifecycleManager lifecycleManager;
    private final ChatAgentExecutor chatAgentExecutor;

    public AsyncChatAgentClient(
            ChatTurnLifecycleManager lifecycleManager,
            ChatAgentExecutor chatAgentExecutor
    ) {
        this.lifecycleManager = lifecycleManager;
        this.chatAgentExecutor = chatAgentExecutor;
    }

    @Async
    @Override
    public void executeAsync(String turnId) {
        long startedAt = System.nanoTime();
        try {
            AgentExecutionCommand command = lifecycleManager.markProcessing(turnId);
            AgentExecutionResult result = chatAgentExecutor.execute(command);
            lifecycleManager.markSuccess(turnId, result, elapsedMs(startedAt));
        } catch (Exception exception) {
            lifecycleManager.markFailed(turnId, exception, elapsedMs(startedAt));
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

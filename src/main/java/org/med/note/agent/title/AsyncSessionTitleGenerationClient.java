package org.med.note.agent.title;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring 异步执行器的会话标题生成客户端。
 *
 * <p>该流程独立于主对话 Agent：标题生成失败只更新标题状态，不改变轮次执行结果。</p>
 */
@Component
public class AsyncSessionTitleGenerationClient implements SessionTitleGenerationClient {

    private final SessionTitleLifecycleManager lifecycleManager;
    private final SessionTitleSourceLoader titleSourceLoader;
    private final SessionTitleGenerator titleGenerator;

    public AsyncSessionTitleGenerationClient(
            SessionTitleLifecycleManager lifecycleManager,
            SessionTitleSourceLoader titleSourceLoader,
            SessionTitleGenerator titleGenerator
    ) {
        this.lifecycleManager = lifecycleManager;
        this.titleSourceLoader = titleSourceLoader;
        this.titleGenerator = titleGenerator;
    }

    @Async
    @Override
    public void generateAsync(String sessionId, String sourceTurnId) {
        try {
            lifecycleManager.markGenerating(sessionId);
            String userInput = titleSourceLoader.loadUserInput(sourceTurnId);
            String title = titleGenerator.generate(userInput);
            lifecycleManager.markGenerated(sessionId, title);
        } catch (Exception exception) {
            lifecycleManager.markFailed(sessionId, exception);
        }
    }
}

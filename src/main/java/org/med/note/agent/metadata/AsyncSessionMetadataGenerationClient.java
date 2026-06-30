package org.med.note.agent.metadata;

import org.med.note.domain.metadata.SessionMetadataResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring 异步执行器的会话元数据生成客户端。
 *
 * <p>元数据生成失败只更新元数据状态，不改变单轮 Agent 执行结果。</p>
 */
@Component
public class AsyncSessionMetadataGenerationClient implements SessionMetadataGenerationClient {

    private final SessionMetadataLifecycleManager lifecycleManager;
    private final SessionMetadataSourceLoader sourceLoader;
    private final SessionMetadataGenerationOrchestrator orchestrator;

    public AsyncSessionMetadataGenerationClient(
            SessionMetadataLifecycleManager lifecycleManager,
            SessionMetadataSourceLoader sourceLoader,
            SessionMetadataGenerationOrchestrator orchestrator
    ) {
        this.lifecycleManager = lifecycleManager;
        this.sourceLoader = sourceLoader;
        this.orchestrator = orchestrator;
    }

    @Async
    @Override
    public void generateAsync(String sessionId, String sourceTurnId) {
        try {
            lifecycleManager.markGenerating(sessionId, sourceTurnId);
            SessionMetadataContext context = sourceLoader.load(sessionId, sourceTurnId);
            SessionMetadataResult result = orchestrator.generate(context);
            lifecycleManager.markGenerated(sessionId, sourceTurnId, result);
        } catch (Exception exception) {
            lifecycleManager.markFailed(sessionId, sourceTurnId, exception);
        }
    }
}

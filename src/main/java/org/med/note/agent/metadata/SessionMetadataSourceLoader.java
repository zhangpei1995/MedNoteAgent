package org.med.note.agent.metadata;

/**
 * 会话元数据来源加载器。
 */
public interface SessionMetadataSourceLoader {

    /**
     * 加载元数据分析所需上下文。
     *
     * @param sessionId 会话 ID
     * @param sourceTurnId 来源轮次 ID
     * @return 可供分析项共享的上下文
     */
    SessionMetadataContext load(String sessionId, String sourceTurnId);
}

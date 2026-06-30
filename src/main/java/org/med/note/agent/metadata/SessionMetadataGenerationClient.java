package org.med.note.agent.metadata;

/**
 * 会话元数据异步生成客户端。
 */
public interface SessionMetadataGenerationClient {

    /**
     * 异步生成会话元数据。
     *
     * @param sessionId 会话 ID
     * @param sourceTurnId 元数据来源轮次 ID
     */
    void generateAsync(String sessionId, String sourceTurnId);
}

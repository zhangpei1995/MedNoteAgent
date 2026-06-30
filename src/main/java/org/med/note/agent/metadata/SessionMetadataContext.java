package org.med.note.agent.metadata;

import lombok.Data;

/**
 * 会话元数据分析上下文。
 *
 * <p>上下文由来源加载器构建，供所有分析项共享；分析项不得自行访问数据库。</p>
 */
@Data
public class SessionMetadataContext {

    /**
     * 当前会话 ID。
     */
    private String sessionId;

    /**
     * 元数据来源轮次 ID。
     */
    private String sourceTurnId;

    /**
     * 来源轮次的用户原始输入。
     */
    private String userInput;
}

package org.med.note.domain.metadata;

/**
 * 会话元数据异步生成状态。
 *
 * <p>元数据生成独立于单轮 Agent 回答；失败时不影响轮次审计和回答回写。</p>
 */
public enum SessionMetadataStatus {

    GENERATING,
    GENERATED,
    FAILED
}

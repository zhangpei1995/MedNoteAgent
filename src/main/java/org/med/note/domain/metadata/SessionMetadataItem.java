package org.med.note.domain.metadata;

/**
 * 会话元数据分析项。
 *
 * <p>每个分析项可以拥有独立 Prompt、输出结构和校验逻辑，由统一编排器调度。</p>
 */
public enum SessionMetadataItem {

    SCOPE_BOUNDARY,
    RETRIEVAL_TARGET,
    CONSULTATION_CATEGORY,
    TITLE
}

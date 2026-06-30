package org.med.note.domain.metadata;

/**
 * 单个分析项生成的会话元数据片段。
 */
public interface SessionMetadataFragment {

    /**
     * 将当前片段合并到最终元数据结果。
     *
     * @param result 待合并的元数据结果，不应为空
     */
    void applyTo(SessionMetadataResult result);
}

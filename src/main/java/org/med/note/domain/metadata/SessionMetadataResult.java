package org.med.note.domain.metadata;

import lombok.Data;

/**
 * 会话元数据生成结果。
 *
 * <p>该对象是多个分析项合并后的强类型结果，落库前应完成清洗、枚举校验和医学边界校验。</p>
 */
@Data
public class SessionMetadataResult {

    private String title;

    private ConsultationCategory consultationCategory;

    private String recognizedDrugName;

    private String instructionItem;

    private KnowledgeStatus knowledgeStatus;

    private ScopeStatus scopeStatus;

    private String understandingText;
}

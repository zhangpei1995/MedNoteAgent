package org.med.note.domain.metadata;

import lombok.Getter;

/**
 * 药品说明书知识库命中状态。
 */
@Getter
public enum KnowledgeStatus {

    PENDING_RETRIEVAL("等待条目检索"),
    DRUG_IDENTIFIED("已识别药品"),
    UNKNOWN_DRUG("未知药品"),
    NOT_INCLUDED("说明未录入"),
    ITEM_NOT_FOUND("条目未命中");

    private final String label;

    KnowledgeStatus(String label) {
        this.label = label;
    }
}

package org.med.note.domain.metadata;

import lombok.Getter;

/**
 * 用户问题是否处于当前药品说明书事实检索边界内。
 */
@Getter
public enum ScopeStatus {

    IN_SCOPE("范围内"),
    OUT_OF_SCOPE("超出范围"),
    NEED_MORE_INFO("需补充信息");

    private final String label;

    ScopeStatus(String label) {
        this.label = label;
    }
}

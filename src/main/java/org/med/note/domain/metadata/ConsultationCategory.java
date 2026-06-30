package org.med.note.domain.metadata;

import lombok.Getter;

/**
 * 药品说明书检索会话类别。
 *
 * <p>类别用于会话入口展示、筛选和统计，不代表医学诊断结论。</p>
 */
@Getter
public enum ConsultationCategory {

    DRUG_LOOKUP("药品检索", "查询药品名称、药品基础信息或是否收录"),
    USAGE_DOSAGE("用法用量", "查询用法、剂量、频次、疗程"),
    CONTRAINDICATION("禁忌慎用", "查询禁忌、慎用、不适用人群"),
    ADVERSE_REACTION("不良反应", "查询副作用、不良反应、安全风险"),
    INTERACTION("相互作用", "查询药物、食物、酒精等相互作用"),
    PRECAUTION("注意事项", "查询孕哺、儿童、老人、肝肾功能等注意事项"),
    INSTRUCTION_ITEM("说明书条目", "查询成分、规格、贮藏、有效期等说明书条目"),
    OUT_OF_SCOPE("超出范围", "诊断、治疗方案或个体化用药判断");

    private final String label;
    private final String description;

    ConsultationCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }
}

package org.med.note.agent.metadata.analyzer;

import cn.hutool.json.JSONObject;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.med.note.agent.metadata.SessionMetadataAnalyzer;
import org.med.note.agent.metadata.SessionMetadataContext;
import org.med.note.domain.metadata.ConsultationCategory;
import org.med.note.domain.metadata.ScopeStatus;
import org.med.note.domain.metadata.SessionMetadataItem;
import org.med.note.domain.metadata.SessionMetadataResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生成会话咨询类别。
 */
@Component
public class ConsultationCategoryMetadataAnalyzer implements SessionMetadataAnalyzer<MetadataFragments.ConsultationCategoryValue> {

    @Override
    public SessionMetadataItem item() {
        return SessionMetadataItem.CONSULTATION_CATEGORY;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean supports(SessionMetadataContext context, SessionMetadataResult currentResult) {
        return true;
    }

    @Override
    public List<Message> buildMessages(SessionMetadataContext context, SessionMetadataResult currentResult) {
        String prompt = """
                你是药品说明书检索系统的咨询类别分析器。
                任务：只根据用户问题和已识别上下文选择一个最匹配的类别 code。

                类别 code：
                - DRUG_LOOKUP：查询药品名称、药品基础信息或是否收录
                - USAGE_DOSAGE：查询用法、剂量、频次、疗程
                - CONTRAINDICATION：查询禁忌、慎用、不适用人群
                - ADVERSE_REACTION：查询副作用、不良反应、安全风险
                - INTERACTION：查询药物、食物、酒精等相互作用
                - PRECAUTION：查询孕哺、儿童、老人、肝肾功能等注意事项
                - INSTRUCTION_ITEM：查询成分、规格、贮藏、有效期等说明书条目
                - OUT_OF_SCOPE：诊断、治疗方案或个体化用药判断

                当前范围状态：%s
                已识别药品：%s
                已识别条目：%s

                只返回 JSON：
                {"consultationCategory":"USAGE_DOSAGE"}

                用户问题：
                %s
                """.formatted(
                currentResult.getScopeStatus(),
                currentResult.getRecognizedDrugName(),
                currentResult.getInstructionItem(),
                context.getUserInput()
        );
        return List.of(Message.builder().role(Role.USER.getValue()).content(prompt).build());
    }

    @Override
    public MetadataFragments.ConsultationCategoryValue parse(String rawOutput, SessionMetadataContext context, SessionMetadataResult currentResult) {
        JSONObject json = AnalyzerSupport.parseObject(rawOutput);
        ConsultationCategory defaultCategory = currentResult.getScopeStatus() == ScopeStatus.OUT_OF_SCOPE
                ? ConsultationCategory.OUT_OF_SCOPE
                : ConsultationCategory.DRUG_LOOKUP;
        MetadataFragments.ConsultationCategoryValue fragment = new MetadataFragments.ConsultationCategoryValue();
        fragment.setConsultationCategory(AnalyzerSupport.enumOrDefault(
                ConsultationCategory.class,
                json.getStr("consultationCategory"),
                defaultCategory
        ));
        return fragment;
    }
}

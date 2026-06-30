package org.med.note.agent.metadata.analyzer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.med.note.agent.metadata.SessionMetadataAnalyzer;
import org.med.note.agent.metadata.SessionMetadataContext;
import org.med.note.domain.metadata.KnowledgeStatus;
import org.med.note.domain.metadata.ScopeStatus;
import org.med.note.domain.metadata.SessionMetadataItem;
import org.med.note.domain.metadata.SessionMetadataResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 识别用户问题中的药品名、说明书条目和初始收录状态。
 */
@Component
public class RetrievalTargetMetadataAnalyzer implements SessionMetadataAnalyzer<MetadataFragments.RetrievalTarget> {

    private static final int MAX_UNDERSTANDING_LENGTH = 120;

    @Override
    public SessionMetadataItem item() {
        return SessionMetadataItem.RETRIEVAL_TARGET;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean supports(SessionMetadataContext context, SessionMetadataResult currentResult) {
        return currentResult.getScopeStatus() != ScopeStatus.OUT_OF_SCOPE;
    }

    @Override
    public List<Message> buildMessages(SessionMetadataContext context, SessionMetadataResult currentResult) {
        String prompt = """
                你是药品说明书检索系统的检索对象分析器。
                任务：从用户问题中识别药品名称、说明书条目和当前初始知识状态。

                已录入药品线索当前只有：二冬汤颗粒、菖麻熄风颗粒。
                如果用户药品名称不明确，recognizedDrugName 返回空字符串，knowledgeStatus 返回 UNKNOWN_DRUG。
                如果用户给出明确药品但不在已录入线索中，knowledgeStatus 返回 NOT_INCLUDED。
                如果药品可进入检索，knowledgeStatus 返回 PENDING_RETRIEVAL。

                instructionItem 使用简短中文条目，例如：用法用量、禁忌慎用、不良反应、相互作用、注意事项、成分、贮藏、有效期；无法识别时为空字符串。
                understandingText 用一句中文说明系统对检索对象的理解，不要给医学建议。

                可选 knowledgeStatus：PENDING_RETRIEVAL、DRUG_IDENTIFIED、UNKNOWN_DRUG、NOT_INCLUDED、ITEM_NOT_FOUND

                只返回 JSON：
                {"recognizedDrugName":"二冬汤颗粒","instructionItem":"用法用量","knowledgeStatus":"PENDING_RETRIEVAL","understandingText":"用户想查询二冬汤颗粒的用法用量，应按已录入说明书检索。"}

                用户问题：
                %s
                """.formatted(context.getUserInput());
        return List.of(Message.builder().role(Role.USER.getValue()).content(prompt).build());
    }

    @Override
    public MetadataFragments.RetrievalTarget parse(String rawOutput, SessionMetadataContext context, SessionMetadataResult currentResult) {
        JSONObject json = AnalyzerSupport.parseObject(rawOutput);
        MetadataFragments.RetrievalTarget fragment = new MetadataFragments.RetrievalTarget();
        fragment.setRecognizedDrugName(StrUtil.blankToDefault(json.getStr("recognizedDrugName"), null));
        fragment.setInstructionItem(StrUtil.blankToDefault(json.getStr("instructionItem"), null));
        fragment.setKnowledgeStatus(AnalyzerSupport.enumOrDefault(KnowledgeStatus.class, json.getStr("knowledgeStatus"), KnowledgeStatus.PENDING_RETRIEVAL));
        fragment.setUnderstandingText(StrUtil.maxLength(
                StrUtil.blankToDefault(json.getStr("understandingText"), "系统正在根据用户输入分析药品和说明书条目。"),
                MAX_UNDERSTANDING_LENGTH
        ));
        return fragment;
    }
}

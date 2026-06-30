package org.med.note.agent.metadata.analyzer;

import cn.hutool.json.JSONObject;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.med.note.agent.metadata.SessionMetadataAnalyzer;
import org.med.note.agent.metadata.SessionMetadataContext;
import org.med.note.domain.metadata.ScopeStatus;
import org.med.note.domain.metadata.SessionMetadataItem;
import org.med.note.domain.metadata.SessionMetadataResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 判断用户问题是否处于药品说明书事实检索范围内。
 */
@Component
public class ScopeBoundaryMetadataAnalyzer implements SessionMetadataAnalyzer<MetadataFragments.ScopeBoundary> {

    @Override
    public SessionMetadataItem item() {
        return SessionMetadataItem.SCOPE_BOUNDARY;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean supports(SessionMetadataContext context, SessionMetadataResult currentResult) {
        return true;
    }

    @Override
    public List<Message> buildMessages(SessionMetadataContext context, SessionMetadataResult currentResult) {
        String prompt = """
                你是药品说明书检索系统的范围边界分析器。
                任务：判断用户问题是否只要求查询药品说明书事实。

                可选 scopeStatus：
                - IN_SCOPE：药品名称、用法用量、禁忌、注意事项、不良反应、相互作用、成分、规格、贮藏、有效期等说明书事实检索。
                - OUT_OF_SCOPE：疾病诊断、治疗方案、个体化用药判断、是否应该服药、替代医生或药师建议。
                - NEED_MORE_INFO：用户没有给出足够药品或条目信息，无法进入明确检索。

                只返回 JSON，不要输出解释：
                {"scopeStatus":"IN_SCOPE"}

                用户问题：
                %s
                """.formatted(context.getUserInput());
        return List.of(Message.builder().role(Role.USER.getValue()).content(prompt).build());
    }

    @Override
    public MetadataFragments.ScopeBoundary parse(String rawOutput, SessionMetadataContext context, SessionMetadataResult currentResult) {
        JSONObject json = AnalyzerSupport.parseObject(rawOutput);
        MetadataFragments.ScopeBoundary fragment = new MetadataFragments.ScopeBoundary();
        fragment.setScopeStatus(AnalyzerSupport.enumOrDefault(ScopeStatus.class, json.getStr("scopeStatus"), ScopeStatus.NEED_MORE_INFO));
        return fragment;
    }
}

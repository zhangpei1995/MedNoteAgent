package org.med.note.agent.metadata.analyzer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.med.note.agent.metadata.SessionMetadataAnalyzer;
import org.med.note.agent.metadata.SessionMetadataContext;
import org.med.note.domain.metadata.SessionMetadataItem;
import org.med.note.domain.metadata.SessionMetadataResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生成会话入口短标题。
 */
@Component
public class TitleMetadataAnalyzer implements SessionMetadataAnalyzer<MetadataFragments.Title> {

    private static final int MAX_TITLE_LENGTH = 30;

    @Override
    public SessionMetadataItem item() {
        return SessionMetadataItem.TITLE;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean supports(SessionMetadataContext context, SessionMetadataResult currentResult) {
        return true;
    }

    @Override
    public List<Message> buildMessages(SessionMetadataContext context, SessionMetadataResult currentResult) {
        String prompt = """
                你是药品说明书检索系统的会话标题生成器。
                任务：根据用户问题和已识别元数据生成中文短标题。

                要求：
                - 8 到 20 个汉字
                - 使用主题短语，不要复述完整问题
                - 保留药品名、说明书条目或核心检索意图
                - 不包含诊断结论
                - 不包含治疗建议
                - 不使用标点符号

                已识别类别：%s
                已识别药品：%s
                已识别条目：%s
                范围状态：%s

                只返回 JSON：
                {"title":"二冬汤颗粒用法用量"}

                用户问题：
                %s
                """.formatted(
                currentResult.getConsultationCategory(),
                currentResult.getRecognizedDrugName(),
                currentResult.getInstructionItem(),
                currentResult.getScopeStatus(),
                context.getUserInput()
        );
        return List.of(Message.builder().role(Role.USER.getValue()).content(prompt).build());
    }

    @Override
    public MetadataFragments.Title parse(String rawOutput, SessionMetadataContext context, SessionMetadataResult currentResult) {
        JSONObject json = AnalyzerSupport.parseObject(rawOutput);
        MetadataFragments.Title fragment = new MetadataFragments.Title();
        fragment.setTitle(sanitizeTitle(json.getStr("title")));
        return fragment;
    }

    private String sanitizeTitle(String rawTitle) {
        String title = StrUtil.trimToEmpty(rawTitle)
                .replace("“", "")
                .replace("”", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("，", "")
                .replace(",", "")
                .replace("。", "")
                .replace("？", "")
                .replace("?", "")
                .replace("：", "")
                .replace(":", "")
                .replace("\r", "")
                .replace("\n", "");
        title = StrUtil.maxLength(title, MAX_TITLE_LENGTH);
        if (StrUtil.isBlank(title)) {
            throw new IllegalStateException("模型返回的会话标题为空");
        }
        return title;
    }
}

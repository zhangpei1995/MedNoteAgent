package org.med.note.agent.title;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.protocol.Protocol;
import org.med.note.llm.QwenProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 DashScope Qwen 小模型的会话标题生成策略。
 *
 * <p>该策略只生成短标题，不承担医学问答、诊断判断或治疗建议职责。</p>
 */
@Component
public class DashscopeSessionTitleGenerator implements SessionTitleGenerator {

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String TITLE_MODEL = "qwen-turbo";
    private static final int MAX_TITLE_LENGTH = 30;
    private final Generation generation = new Generation(Protocol.HTTP.getValue(), API_URL);

    @Override
    public String generate(String userInput) {
        try {
            GenerationParam param = GenerationParam.builder()
                    .apiKey(QwenProperties.getApiKey())
                    .model(TITLE_MODEL)
                    .messages(buildMessages(userInput))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult generationResult = generation.call(param);
            String rawTitle = generationResult.getOutput()
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            return sanitizeTitle(rawTitle);
        } catch (Exception exception) {
            throw new IllegalStateException("调用 Qwen 生成会话标题失败", exception);
        }
    }

    private List<Message> buildMessages(String userInput) {
        String prompt = """
                请根据用户问题生成一个中文会话标题。
                要求：
                - 8 到 20 个汉字
                - 使用主题短语，不要复述完整问题
                - 保留核心医学主题、药品名或说明书条目
                - 不要包含诊断结论
                - 不要包含治疗建议
                - 不要使用引号、逗号、句号、问号
                - 只返回标题文本

                示例：
                用户问题：尿蛋白是什么原因，炎症吗？
                标题：尿蛋白原因评估

                用户问题：
                %s
                """.formatted(userInput);

        return List.of(Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build());
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
                .replace("\r", "")
                .replace("\n", "");
        title = StrUtil.maxLength(title, MAX_TITLE_LENGTH);
        if (StrUtil.isBlank(title)) {
            throw new IllegalStateException("模型返回的会话标题为空");
        }
        return title;
    }
}

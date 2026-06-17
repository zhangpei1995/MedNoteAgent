package org.med.note.client;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import org.med.note.config.DotenvConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云百炼-千问通用客户端
 * 支持动态系统提示、用户提问、模型切换、统一异常捕获
 */
@Component
public class QianwenClient {

    /**
     * 接口地址
     */
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final Generation GENERATION_CLIENT;

    static {
        GENERATION_CLIENT = new Generation(Protocol.HTTP.getValue(), API_URL);
    }

    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public QianwenClient(
            @Value("${mednote.llm.dashscope.model:qwen-max}") String model,
            @Value("${mednote.llm.dashscope.enabled:false}") boolean enabled
    ) {
        this.apiKey = DotenvConfig.getQwenApiKey();
        this.model = model;
        this.enabled = enabled;
    }

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String chatWithConfiguredModel(String systemPrompt, String userContent)
            throws ApiException, NoApiKeyException, InputRequiredException {
        if (!isConfigured()) {
            throw new NoApiKeyException();
        }
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(buildMessages(systemPrompt, userContent))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = GENERATION_CLIENT.call(param);
        return parseResult(result);
    }

    private static List<Message> buildMessages(String systemPrompt, String userContent) {
        List<Message> messageList = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messageList.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build());
        }
        messageList.add(Message.builder()
                .role(Role.USER.getValue())
                .content(userContent)
                .build());
        return messageList;
    }

    /**
     * 统一解析返回结果，提取回答文本。
     */
    private static String parseResult(GenerationResult result) {
        if (result == null || result.getOutput() == null
                || CollUtil.isEmpty(result.getOutput().getChoices())) {
            return "";
        }
        Message answerMsg = result.getOutput().getChoices().get(0).getMessage();
        return answerMsg == null ? "" : answerMsg.getContent();
    }

}

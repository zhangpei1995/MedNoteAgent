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
import java.util.function.Consumer;

/**
 * 阿里云百炼-千问通用客户端
 * 支持动态系统提示、用户提问、模型切换、统一异常捕获
 */
@Component
public class QianwenClient {

    // ==================== 全局配置项（统一维护，方便修改） ====================
    /**
     * 接口地址
     */
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    /**
     * 默认模型
     */
    private static final String DEFAULT_MODEL = "qwen-max";
    // 初始化客户端实例（单例复用，避免重复创建连接）
    private static final Generation GENERATION_CLIENT;

    static {
        GENERATION_CLIENT = new Generation(Protocol.HTTP.getValue(), API_URL);
    }

    private final String apiKey;
    private final String model;

    public QianwenClient(
            @Value("${mednote.llm.dashscope.model:qwen-max}") String model
    ) {
        this.apiKey = blankToNull(DotenvConfig.getQwenApiKey());
        this.model = blankToDefault(model, DEFAULT_MODEL);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chatWithConfiguredModel(String systemPrompt, String userContent)
            throws ApiException, NoApiKeyException, InputRequiredException {
        if (!isConfigured()) {
            throw new NoApiKeyException();
        }
        return chat(systemPrompt, userContent, model, apiKey);
    }

    /**
     * 流式调用当前配置模型。
     *
     * <p>根据阿里云 Model Studio 文档，DashScope Java SDK 通过
     * {@code Generation#streamCall} 开启 SSE 流式输出，并建议设置
     * {@code incrementalOutput(true)}，使每个 chunk 只包含新增文本。</p>
     */
    public String streamChatWithConfiguredModel(String systemPrompt, String userContent, Consumer<String> chunkConsumer)
            throws ApiException, NoApiKeyException, InputRequiredException {
        if (!isConfigured()) {
            throw new NoApiKeyException();
        }
        return streamChat(systemPrompt, userContent, model, apiKey, chunkConsumer);
    }

    private static String chat(String systemPrompt, String userContent, String model, String apiKey)
            throws ApiException, NoApiKeyException, InputRequiredException {
        if (apiKey == null || apiKey.isBlank()) {
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

    private static String streamChat(String systemPrompt, String userContent, String model, String apiKey, Consumer<String> chunkConsumer)
            throws ApiException, NoApiKeyException, InputRequiredException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new NoApiKeyException();
        }
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(buildMessages(systemPrompt, userContent))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true)
                .build();

        StringBuilder fullContent = new StringBuilder();
        GENERATION_CLIENT.streamCall(param).blockingForEach(result -> {
            String chunk = parseResult(result);
            if (chunk != null && !chunk.isBlank()) {
                fullContent.append(chunk);
                if (chunkConsumer != null) {
                    chunkConsumer.accept(chunk);
                }
            }
        });
        return fullContent.toString();
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

    // ==================== 内部工具方法 ====================

    /**
     * 统一解析返回结果，提取回答文本
     */
    private static String parseResult(GenerationResult result) {
        if (result == null || result.getOutput() == null
                || CollUtil.isEmpty(result.getOutput().getChoices())) {
            return "";
        }
        Message answerMsg = result.getOutput().getChoices().get(0).getMessage();
        return answerMsg == null ? "" : answerMsg.getContent();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

}

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

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云百炼-千问通用客户端
 * 支持动态系统提示、用户提问、模型切换、统一异常捕获
 */
public class QianwenClient {

    // ==================== 全局配置项（统一维护，方便修改） ====================
    /**
     * API Key 建议生产环境放入环境变量/配置中心，不要硬编码
     */
    private static final String API_KEY = "sk-d280227b00c442d9bbbd2d054908bfad";
    /**
     * 接口地址
     */
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    /**
     * 默认模型
     */
    private static final String DEFAULT_MODEL = "qwen3.7-max";
    /**
     * 默认系统角色提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    // 初始化客户端实例（单例复用，避免重复创建连接）
    private static final Generation GENERATION_CLIENT;

    static {
        GENERATION_CLIENT = new Generation(Protocol.HTTP.getValue(), API_URL);
    }

    // ==================== 对外通用方法 ====================

    /**
     * 简易调用：仅传入用户问题，使用默认系统提示词
     *
     * @param userContent 用户提问内容
     * @return 模型回复文本
     */
    public static String chat(String userContent)
            throws ApiException, NoApiKeyException, InputRequiredException {
        return chat(DEFAULT_SYSTEM_PROMPT, userContent, DEFAULT_MODEL);
    }

    /**
     * 自定义系统提示词 + 用户问题
     *
     * @param systemPrompt 系统角色设定
     * @param userContent  用户提问
     * @return 模型回复文本
     */
    public static String chat(String systemPrompt, String userContent)
            throws ApiException, NoApiKeyException, InputRequiredException {
        return chat(systemPrompt, userContent, DEFAULT_MODEL);
    }

    /**
     * 全参数自定义：系统提示、用户提问、指定模型
     *
     * @param systemPrompt 系统角色设定
     * @param userContent  用户提问
     * @param model        模型名称
     * @return 模型回复文本
     */
    public static String chat(String systemPrompt, String userContent, String model)
            throws ApiException, NoApiKeyException, InputRequiredException {
        // 构建消息列表
        List<Message> messageList = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build();
            messageList.add(systemMsg);
        }

        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userContent)
                .build();
        messageList.add(userMsg);

        // 构造请求参数
        GenerationParam param = GenerationParam.builder()
                .apiKey(API_KEY)
                .model(model)
                .messages(messageList)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        // 调用接口并解析结果
        GenerationResult result = GENERATION_CLIENT.call(param);
        return parseResult(result);
    }

    /**
     * 多轮对话：传入完整消息上下文
     *
     * @param messageList 对话消息列表(包含历史对话)
     * @param model       模型名称
     * @return 模型回复文本
     */
    public static String multiTurnChat(List<Message> messageList, String model)
            throws ApiException, NoApiKeyException, InputRequiredException {
        if (CollUtil.isEmpty(messageList)) {
            throw new InputRequiredException("对话消息列表不能为空");
        }
        GenerationParam param = GenerationParam.builder()
                .apiKey(API_KEY)
                .model(model)
                .messages(messageList)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        GenerationResult result = GENERATION_CLIENT.call(param);
        return parseResult(result);
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

    // ==================== 测试入口 ====================
    public static void main(String[] args) {
        try {
            // 测试1：最简调用
            String answer1 = chat("你是谁？");
            System.out.println("回答1：" + answer1);

            // 测试2：自定义系统提示词
            String answer2 = chat("你是一名Java技术专家，回答尽量简洁", "解释什么是Java接口");
            System.out.println("回答2：" + answer2);

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.err.println("调用千问接口异常：" + e.getMessage());
            System.err.println("错误文档参考：https://help.aliyun.com/model-studio/developer-reference/error-code");
        }
    }
}
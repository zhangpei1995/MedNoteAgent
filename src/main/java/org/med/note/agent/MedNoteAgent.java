package org.med.note.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.protocol.Protocol;
import org.med.note.llm.QwenProperties;

public class MedNoteAgent {

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final Generation generation = new Generation(Protocol.HTTP.getValue(), API_URL);

    public static String chat(String userInput) {
        try {

            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个严谨、可靠的医学笔记助手。")
                    .build();

            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userInput)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(QwenProperties.getApiKey())
                    .model(QwenProperties.getDefaultModel())
                    .messages(CollUtil.newArrayList(systemMessage, userMessage))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = generation.call(param);

            return result.getOutput()
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            throw new IllegalStateException("调用 Qwen 模型失败", e);
        }
    }

    public static void main(String[] args) {
        String result = chat("孕妇可以吃阿莫西林吗？");
        System.out.println(result);
    }

}

package org.med.note.agent.metadata;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.protocol.Protocol;
import org.med.note.llm.QwenProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 DashScope Qwen 的会话元数据分析模型客户端。
 */
@Component
public class DashscopeSessionMetadataModelClient implements SessionMetadataModelClient {

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String MODEL = "qwen-turbo";
    private final Generation generation = new Generation(Protocol.HTTP.getValue(), API_URL);

    @Override
    public String call(List<Message> messages) {
        try {
            GenerationParam param = GenerationParam.builder()
                    .apiKey(QwenProperties.getApiKey())
                    .model(MODEL)
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult generationResult = generation.call(param);
            return generationResult.getOutput()
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception exception) {
            throw new IllegalStateException("调用 Qwen 生成会话元数据失败", exception);
        }
    }
}

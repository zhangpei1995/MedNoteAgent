package org.med.note.agent.execution;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.protocol.Protocol;
import org.med.note.llm.QwenProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于 DashScope Qwen 模型的医学问答 Agent 执行策略。
 */
@Component
public class DashscopeChatAgentExecutor implements ChatAgentExecutor {

    private static final String MODEL_PROVIDER = "dashscope";
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final Path AGENT_PROMPT_PATH = Path.of(
            "src/main/java/org/med/note/agent/AgentPrompt.md"
    );

    private final Generation generation = new Generation(Protocol.HTTP.getValue(), API_URL);

    @Override
    public AgentExecutionResult execute(AgentExecutionCommand command) {
        try {
            String systemPrompt = readAgentPrompt();
            String modelName = QwenProperties.getDefaultModel();

            Message systemMessage = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build();
            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(command.getUserInput())
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(QwenProperties.getApiKey())
                    .model(modelName)
                    .messages(CollUtil.newArrayList(systemMessage, userMessage))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult generationResult = generation.call(param);
            return buildResult(systemPrompt, modelName, param, generationResult);
        } catch (Exception exception) {
            throw new IllegalStateException("调用 Qwen 模型失败", exception);
        }
    }

    private AgentExecutionResult buildResult(
            String systemPrompt,
            String modelName,
            GenerationParam param,
            GenerationResult generationResult
    ) {
        AgentExecutionResult result = new AgentExecutionResult();
        result.setAssistantOutput(generationResult.getOutput()
                .getChoices()
                .get(0)
                .getMessage()
                .getContent());
        result.setModelProvider(MODEL_PROVIDER);
        result.setModelName(modelName);
        result.setSystemPrompt(systemPrompt);
        result.setRequestJson(JSONUtil.toJsonStr(param));
        result.setResponseJson(JSONUtil.toJsonStr(generationResult));
        return result;
    }

    private String readAgentPrompt() throws IOException {
        return Files.readString(AGENT_PROMPT_PATH, StandardCharsets.UTF_8);
    }
}

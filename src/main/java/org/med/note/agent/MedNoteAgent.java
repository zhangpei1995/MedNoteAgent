package org.med.note.agent;

import cn.hutool.core.exceptions.ValidateException;
import org.med.note.client.QianwenClient;
import org.med.note.dto.AgentRequest;
import org.med.note.dto.AgentResponse;
import org.med.note.dto.AgentStep;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MedNoteAgent {

    QianwenClient qianwenClient;

    public MedNoteAgent(QianwenClient qianwenClient) {
        this.qianwenClient = qianwenClient;
    }

    public AgentResponse run(AgentRequest request) {
        AgentRequest safeRequest = request == null ? AgentRequest.empty() : request;
        String input = normalizeInput(safeRequest.input());
        List<AgentStep> steps = runLoop(input);
        return new AgentResponse(
                "最小 Agent 循环已接收输入，功能逻辑留待扩展。",
                steps,
                Instant.now()
        );
    }

    private List<AgentStep> runLoop(String input) {
        List<AgentStep> steps = new ArrayList<>();
        List<String> queue = new ArrayList<>();
        queue.add(input);


        for (int index = 0; index < queue.size(); index++) {
            String currentInput = queue.get(index);
            steps.add(new AgentStep(
                    index + 1,
                    "loop",
                    "已接收并规范化输入：" + currentInput
            ));
        }
        steps.add(new AgentStep(
                steps.size() + 1,
                "final",
                "最小 Agent 循环完成，等待接入后续医学工具链。"
        ));

        return steps;
    }

    private String normalizeInput(String input) {
        if (input == null || input.isBlank()) {
            throw new ValidateException("input 为空");
        }
        return input.trim();
    }
}

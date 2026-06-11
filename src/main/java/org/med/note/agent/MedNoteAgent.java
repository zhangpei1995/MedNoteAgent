package org.med.note.agent;

import org.med.note.dto.AgentRunRequest;
import org.med.note.dto.AgentRunResponse;
import org.med.note.dto.AgentStep;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MedNoteAgent {

    public AgentRunResponse run(AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        List<AgentStep> steps = buildDemoSteps(safeRequest);
        String summary = "Demo agent completed " + steps.size()
                + " steps for topic: " + normalizeTopic(safeRequest.topic()) + ".";
        return new AgentRunResponse("med-note-demo-agent", summary, steps, Instant.now());
    }

    public List<AgentStep> buildDemoSteps(AgentRunRequest request) {
        AgentRunRequest safeRequest = request == null ? AgentRunRequest.empty() : request;
        String topic = normalizeTopic(safeRequest.topic());
        String input = normalizeInput(safeRequest.input());

        return List.of(
                new AgentStep(1, "intent", "识别任务主题: " + topic),
                new AgentStep(2, "extract", "读取输入并抽取候选医学信息: " + input),
                new AgentStep(3, "reason", "基于 demo 规则生成结构化笔记草稿"),
                new AgentStep(4, "final", "输出可检查的 agent 测试结果")
        );
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "药品说明书结构化摘要";
        }
        return topic.trim();
    }

    private String normalizeInput(String input) {
        if (input == null || input.isBlank()) {
            return "未提供原始内容，使用本地示例输入";
        }
        return input.trim();
    }
}

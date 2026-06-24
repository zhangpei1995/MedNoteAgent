package org.med.note.agent.execution;

import lombok.Data;

/**
 * Agent 执行一轮对话所需的输入命令。
 */
@Data
public class AgentExecutionCommand {

    /**
     * 对话轮次审计 ID，用于追踪本次执行归属。
     */
    private String turnId;

    /**
     * 用户本轮原始输入，必须原样传入 Agent 以保留医学问答语义。
     */
    private String userInput;
}

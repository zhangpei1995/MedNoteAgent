package org.med.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询单轮对话执行情况的返回结果。
 *
 * <p>用于提交后根据 turnId 展示当前轮次状态；Agent 会逐步填充助手输出、
 * 错误信息和完成时间。会话标题由独立标题接口查询。</p>
 */
@Data
public class ChatTurnStatusResponse {

    /**
     * 本轮对话审计 ID。
     */
    private String turnId;

    /**
     * 本轮所属会话 ID。
     */
    private String sessionId;

    /**
     * 本轮执行状态，例如 WAITING_AGENT、PROCESSING、SUCCESS、FAILED。
     */
    private String status;

    /**
     * 用户本轮原始输入。
     */
    private String userInput;

    /**
     * Agent 或系统最终输出。Agent 未执行完成前为空。
     */
    private String assistantOutput;

    /**
     * 执行失败时的错误信息。成功或未执行时为空。
     */
    private String errorMessage;

    /**
     * 本轮记录创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 本轮执行完成时间。Agent 未完成前为空。
     */
    private LocalDateTime completedAt;
}

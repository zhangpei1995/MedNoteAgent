package org.med.note.dto;

import lombok.Data;
import org.med.note.domain.entity.ChatTurnAudit;

import java.time.LocalDateTime;

/**
 * 会话详情中的单轮对话记录。
 *
 * <p>该 DTO 面向会话历史展示，保留用户原文、助手输出、状态、失败原因和时间字段，
 * 便于医学问答链路在前端保持可追溯性。</p>
 */
@Data
public class ChatTurnRecordResponse {

    /**
     * 本轮对话审计 ID。
     */
    private String turnId;

    /**
     * 本轮所属会话 ID。
     */
    private String sessionId;

    /**
     * 用户本轮原始输入。
     */
    private String userInput;

    /**
     * Agent 或系统最终输出。Agent 未完成前为空。
     */
    private String assistantOutput;

    /**
     * 本轮执行状态，例如 WAITING_AGENT、PROCESSING、SUCCESS、FAILED。
     */
    private String status;

    /**
     * 执行失败时的错误信息。成功或未完成时为空。
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

    /**
     * 从轮次审计实体构建会话历史记录。
     *
     * @param turnAudit 单轮审计实体，不应为空
     * @return 前端会话详情可展示的一轮记录
     */
    public static ChatTurnRecordResponse of(ChatTurnAudit turnAudit) {
        ChatTurnRecordResponse response = new ChatTurnRecordResponse();
        response.setTurnId(turnAudit.getId());
        response.setSessionId(turnAudit.getSessionId());
        response.setUserInput(turnAudit.getUserInput());
        response.setAssistantOutput(turnAudit.getAssistantOutput());
        response.setStatus(turnAudit.getStatus());
        response.setErrorMessage(turnAudit.getErrorMessage());
        response.setCreatedAt(turnAudit.getCreatedAt());
        response.setCompletedAt(turnAudit.getCompletedAt());
        return response;
    }
}

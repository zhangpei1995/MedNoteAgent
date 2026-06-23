package org.med.note.dto;

import java.time.LocalDateTime;

/**
 * 查询单轮对话执行情况的返回结果。
 *
 * <p>用于提交后根据 turnId 展示当前轮次状态；Agent 接入后会逐步填充助手输出、
 * 错误信息和完成时间。</p>
 */
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
     * 会话展示标题，用于状态查询页和会话列表。
     */
    private String title;

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

    public String getTurnId() {
        return turnId;
    }

    public void setTurnId(String turnId) {
        this.turnId = turnId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getAssistantOutput() {
        return assistantOutput;
    }

    public void setAssistantOutput(String assistantOutput) {
        this.assistantOutput = assistantOutput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

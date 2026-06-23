package org.med.note.dto;

import java.time.LocalDateTime;

/**
 * 提交聊天输入后的返回结果。
 *
 * <p>调用方应保存 sessionId 用于后续连续对话，保存 turnId 用于查询本轮执行情况。</p>
 */
public class SubmitChatTurnResponse {

    /**
     * 当前会话 ID。首次提交时由服务创建，后续提交时可继续传回。
     */
    private String sessionId;

    /**
     * 本轮对话审计 ID，用于查询 Agent 执行状态和最终输出。
     */
    private String turnId;

    /**
     * 会话展示标题，用于前端会话列表或状态页。
     */
    private String title;

    /**
     * 本轮初始状态。当前阶段为 WAITING_AGENT，表示记录已落库但尚未执行 Agent。
     */
    private String turnStatus;

    /**
     * 本轮记录创建时间。
     */
    private LocalDateTime createdAt;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTurnId() {
        return turnId;
    }

    public void setTurnId(String turnId) {
        this.turnId = turnId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTurnStatus() {
        return turnStatus;
    }

    public void setTurnStatus(String turnStatus) {
        this.turnStatus = turnStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

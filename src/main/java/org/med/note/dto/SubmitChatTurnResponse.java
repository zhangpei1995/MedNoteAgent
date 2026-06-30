package org.med.note.dto;

import lombok.Data;
import org.med.note.agent.runtime.ChatTurnSubmission;
import org.med.note.domain.entity.ChatSession;
import org.med.note.domain.entity.ChatTurnAudit;

import java.time.LocalDateTime;

/**
 * 提交聊天输入后的返回结果。
 *
 * <p>调用方应保存 sessionId 用于后续连续对话，保存 turnId 用于查询本轮执行情况。</p>
 */
@Data
public class SubmitChatTurnResponse {

    /**
     * 当前会话 ID。首次提交时由服务创建，后续提交时可继续传回。
     */
    private String sessionId;

    /**
     * 外部用户 ID。当前阶段可为空，仅用于前端保持会话摘要一致。
     */
    private String userId;

    /**
     * 本轮对话审计 ID，用于查询 Agent 执行状态和最终输出。
     */
    private String turnId;

    /**
     * 会话真实展示标题。新会话标题生成前为空，前端展示默认标题。
     */
    private String title;

    /**
     * 标题生成状态，例如 GENERATING、GENERATED、FAILED。
     */
    private String titleStatus;

    /**
     * 标题生成完成时间；生成中或失败时为空。
     */
    private LocalDateTime titleGeneratedAt;

    /**
     * 会话状态，例如 ACTIVE、ENDED、ERROR。
     */
    private String status;

    /**
     * 会话创建时间。
     */
    private LocalDateTime sessionCreatedAt;

    /**
     * 会话最近更新时间。提交新轮次后更新为本轮提交时间。
     */
    private LocalDateTime sessionUpdatedAt;

    /**
     * 会话结束时间；未结束时为空。
     */
    private LocalDateTime endedAt;

    /**
     * 本轮初始状态。当前阶段为 WAITING_AGENT，表示记录已落库但尚未执行 Agent。
     */
    private String turnStatus;

    /**
     * 本轮记录创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 将聊天提交运行时结果转换为 API 返回 DTO。
     *
     * @param submission 已完成提交编排的运行时结果
     * @return 前端提交后需要保存和展示的会话、轮次与状态信息
     */
    public static SubmitChatTurnResponse of(ChatTurnSubmission submission) {
        return of(submission.getSession(), submission.getTurnAudit());
    }

    /**
     * 根据会话和轮次审计记录构建提交响应。
     *
     * @param session 本轮提交所属会话
     * @param turnAudit 已创建的轮次审计记录
     * @return 提交响应 DTO
     */
    public static SubmitChatTurnResponse of(ChatSession session, ChatTurnAudit turnAudit) {
        SubmitChatTurnResponse response = new SubmitChatTurnResponse();
        response.setSessionId(session.getId());
        response.setUserId(session.getUserId());
        response.setTurnId(turnAudit.getId());
        response.setTitle(session.getTitle());
        response.setTitleStatus(session.getTitleStatus());
        response.setTitleGeneratedAt(session.getTitleGeneratedAt());
        response.setStatus(session.getStatus());
        response.setSessionCreatedAt(session.getCreatedAt());
        response.setSessionUpdatedAt(session.getUpdatedAt());
        response.setEndedAt(session.getEndedAt());
        response.setTurnStatus(turnAudit.getStatus());
        response.setCreatedAt(turnAudit.getCreatedAt());
        return response;
    }
}

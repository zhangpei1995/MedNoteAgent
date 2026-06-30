package org.med.note.dto;

import lombok.Data;
import org.med.note.domain.entity.ChatSession;

import java.time.LocalDateTime;

/**
 * 会话列表中的单条会话摘要。
 *
 * <p>用于前端按最近更新时间展示会话入口，不承载单轮对话内容。需要查看某个会话的完整轮次时，
 * 调用会话轮次查询接口。</p>
 */
@Data
public class ChatSessionSummaryResponse {

    /**
     * 会话 ID，用于继续提交对话或查询会话轮次。
     */
    private String sessionId;

    /**
     * 外部用户 ID。当前阶段可为空，仅用于后续按用户聚合会话。
     */
    private String userId;

    /**
     * 会话真实展示标题；为空时前端展示默认标题。
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
    private LocalDateTime createdAt;

    /**
     * 最近一次追加轮次或更新会话的时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 会话结束时间；未结束时为空。
     */
    private LocalDateTime endedAt;

    /**
     * 从持久化实体构建前端会话摘要。
     *
     * @param session 会话实体，不应为空
     * @return 会话列表可展示的摘要数据
     */
    public static ChatSessionSummaryResponse of(ChatSession session) {
        ChatSessionSummaryResponse response = new ChatSessionSummaryResponse();
        response.setSessionId(session.getId());
        response.setUserId(session.getUserId());
        response.setTitle(session.getTitle());
        response.setTitleStatus(session.getTitleStatus());
        response.setTitleGeneratedAt(session.getTitleGeneratedAt());
        response.setStatus(session.getStatus());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        response.setEndedAt(session.getEndedAt());
        return response;
    }
}

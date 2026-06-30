package org.med.note.dto;

import lombok.Data;
import org.med.note.domain.entity.ChatSession;

import java.time.LocalDateTime;

/**
 * 会话标题异步生成状态。
 *
 * <p>用于前端在会话真实标题尚未生成时查询标题状态。title 为空表示继续展示默认标题；
 * titleStatus 为 GENERATED 时可直接使用 title 更新会话入口。</p>
 */
@Data
public class ChatSessionTitleResponse {

    /**
     * 会话 ID。
     */
    private String sessionId;

    /**
     * 会话真实展示标题；为空表示标题尚未生成或生成失败。
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
     * 从会话实体构建标题查询响应。
     *
     * @param session 会话实体，不应为空
     * @return 当前会话标题状态
     */
    public static ChatSessionTitleResponse of(ChatSession session) {
        ChatSessionTitleResponse response = new ChatSessionTitleResponse();
        response.setSessionId(session.getId());
        response.setTitle(session.getTitle());
        response.setTitleStatus(session.getTitleStatus());
        response.setTitleGeneratedAt(session.getTitleGeneratedAt());
        return response;
    }
}

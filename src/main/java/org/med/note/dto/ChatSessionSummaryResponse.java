package org.med.note.dto;

import lombok.Data;
import org.med.note.domain.entity.ChatSession;
import org.med.note.domain.entity.ChatSessionMetadata;

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

    private String title;

    private String metadataStatus;

    private String consultationCategory;

    private String consultationCategoryLabel;

    private String recognizedDrugName;

    private String instructionItem;

    private String knowledgeStatus;

    private String knowledgeStatusLabel;

    private String scopeStatus;

    private String scopeStatusLabel;

    private String understandingText;

    private LocalDateTime metadataGeneratedAt;

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
    public static ChatSessionSummaryResponse of(ChatSession session, ChatSessionMetadata metadata) {
        ChatSessionSummaryResponse response = new ChatSessionSummaryResponse();
        response.setSessionId(session.getId());
        response.setUserId(session.getUserId());
        if (metadata == null) {
            response.setMetadataStatus("GENERATING");
        } else {
            response.setTitle(metadata.getTitle());
            response.setMetadataStatus(metadata.getStatus());
            response.setConsultationCategory(metadata.getConsultationCategory());
            response.setConsultationCategoryLabel(ChatSessionMetadataResponse.consultationCategoryLabel(metadata.getConsultationCategory()));
            response.setRecognizedDrugName(metadata.getRecognizedDrugName());
            response.setInstructionItem(metadata.getInstructionItem());
            response.setKnowledgeStatus(metadata.getKnowledgeStatus());
            response.setKnowledgeStatusLabel(ChatSessionMetadataResponse.knowledgeStatusLabel(metadata.getKnowledgeStatus()));
            response.setScopeStatus(metadata.getScopeStatus());
            response.setScopeStatusLabel(ChatSessionMetadataResponse.scopeStatusLabel(metadata.getScopeStatus()));
            response.setUnderstandingText(metadata.getUnderstandingText());
            response.setMetadataGeneratedAt(metadata.getGeneratedAt());
        }
        response.setStatus(session.getStatus());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        response.setEndedAt(session.getEndedAt());
        return response;
    }
}

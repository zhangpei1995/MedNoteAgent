package org.med.note.dto;

import lombok.Data;
import org.med.note.domain.entity.ChatSessionMetadata;
import org.med.note.domain.metadata.ConsultationCategory;
import org.med.note.domain.metadata.KnowledgeStatus;
import org.med.note.domain.metadata.ScopeStatus;
import org.med.note.domain.metadata.SessionMetadataStatus;

import java.time.LocalDateTime;

/**
 * 会话元数据异步生成状态和结构化结果。
 */
@Data
public class ChatSessionMetadataResponse {

    private String sessionId;

    private String sourceTurnId;

    private String metadataStatus;

    private String title;

    private String consultationCategory;

    private String consultationCategoryLabel;

    private String recognizedDrugName;

    private String instructionItem;

    private String knowledgeStatus;

    private String knowledgeStatusLabel;

    private String scopeStatus;

    private String scopeStatusLabel;

    private String understandingText;

    private String errorMessage;

    private LocalDateTime generatedAt;

    /**
     * 从实体构建元数据响应；元数据未创建时返回生成中状态。
     */
    public static ChatSessionMetadataResponse of(String sessionId, ChatSessionMetadata metadata) {
        ChatSessionMetadataResponse response = new ChatSessionMetadataResponse();
        response.setSessionId(sessionId);
        if (metadata == null) {
            response.setMetadataStatus(SessionMetadataStatus.GENERATING.name());
            return response;
        }

        response.setSourceTurnId(metadata.getSourceTurnId());
        response.setMetadataStatus(metadata.getStatus());
        response.setTitle(metadata.getTitle());
        response.setConsultationCategory(metadata.getConsultationCategory());
        response.setConsultationCategoryLabel(consultationCategoryLabel(metadata.getConsultationCategory()));
        response.setRecognizedDrugName(metadata.getRecognizedDrugName());
        response.setInstructionItem(metadata.getInstructionItem());
        response.setKnowledgeStatus(metadata.getKnowledgeStatus());
        response.setKnowledgeStatusLabel(knowledgeStatusLabel(metadata.getKnowledgeStatus()));
        response.setScopeStatus(metadata.getScopeStatus());
        response.setScopeStatusLabel(scopeStatusLabel(metadata.getScopeStatus()));
        response.setUnderstandingText(metadata.getUnderstandingText());
        response.setErrorMessage(metadata.getErrorMessage());
        response.setGeneratedAt(metadata.getGeneratedAt());
        return response;
    }

    public static String consultationCategoryLabel(String category) {
        try {
            return category == null ? null : ConsultationCategory.valueOf(category).getLabel();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String knowledgeStatusLabel(String status) {
        try {
            return status == null ? null : KnowledgeStatus.valueOf(status).getLabel();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String scopeStatusLabel(String status) {
        try {
            return status == null ? null : ScopeStatus.valueOf(status).getLabel();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

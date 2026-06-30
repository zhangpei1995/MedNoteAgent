package org.med.note.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话级元数据持久化实体。
 *
 * <p>一条记录对应一个聊天会话，由异步元数据分析链路生成和更新。该实体承载会话入口、
 * 检索上下文面板和后续统计筛选所需的结构化理解结果。</p>
 */
@Data
@TableName("chat_session_metadata")
public class ChatSessionMetadata {

    /**
     * 元数据记录主键。
     */
    @TableId
    private String id;

    /**
     * 所属会话 ID；同一会话仅保留一条当前元数据。
     */
    private String sessionId;

    /**
     * 本次元数据生成来源轮次 ID，便于追溯输入。
     */
    private String sourceTurnId;

    /**
     * 元数据生成状态，例如 GENERATING、GENERATED、FAILED。
     */
    private String status;

    /**
     * 会话入口展示标题。
     */
    private String title;

    /**
     * 会话咨询类别枚举 code。
     */
    private String consultationCategory;

    /**
     * 已识别药品名；无法识别时为空。
     */
    private String recognizedDrugName;

    /**
     * 已识别说明书条目；无法识别时为空。
     */
    private String instructionItem;

    /**
     * 知识库命中状态枚举 code。
     */
    private String knowledgeStatus;

    /**
     * 当前问题是否处于药品说明书事实检索边界内。
     */
    private String scopeStatus;

    /**
     * 面向用户展示的系统理解摘要。
     */
    private String understandingText;

    /**
     * 完整结构化元数据 JSON 快照，用于调试和审计。
     */
    private String metadataJson;

    /**
     * 生成失败时的错误摘要。
     */
    private String errorMessage;

    /**
     * 元数据生成完成时间；生成中或失败时为空。
     */
    private LocalDateTime generatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

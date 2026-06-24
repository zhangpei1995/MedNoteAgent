package org.med.note.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话持久化实体。
 *
 * <p>一条记录代表一次可连续追踪的医学咨询上下文。当前阶段 userId 只保存外部传入标识，
 * 不承担用户画像或行为特征职责。</p>
 */
@Data
@TableName("chat_session")
public class ChatSession {

    /**
     * 会话主键，由服务创建。
     */
    @TableId
    private String id;

    /**
     * 外部用户 ID，可为空；用于后续按用户聚合会话。
     */
    private String userId;

    /**
     * 会话展示标题，首次提交时由显式 title 或用户输入摘要生成。
     */
    private String title;

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
}

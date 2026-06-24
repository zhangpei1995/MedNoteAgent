package org.med.note.agent.runtime;

import lombok.Data;
import org.med.note.domain.entity.ChatSession;
import org.med.note.domain.entity.ChatTurnAudit;

/**
 * 一轮聊天提交完成后的运行时结果。
 *
 * <p>该对象承载已确认的会话和已创建的 WAITING_AGENT 轮次审计记录，
 * 用于服务层或 DTO 层生成 API 返回值。</p>
 */
@Data
public class ChatTurnSubmission {

    /**
     * 本轮提交关联的会话；首次提交时为新建会话，连续对话时为复用会话。
     */
    private ChatSession session;

    /**
     * 已落库且等待 Agent 执行的轮次审计记录。
     */
    private ChatTurnAudit turnAudit;
}

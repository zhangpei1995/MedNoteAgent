package org.med.note.agent.metadata;

import org.med.note.dao.ChatTurnAuditMapper;
import org.med.note.domain.entity.ChatTurnAudit;
import org.springframework.stereotype.Component;

/**
 * 基于来源轮次加载会话元数据分析输入。
 */
@Component
public class FirstTurnMetadataSourceLoader implements SessionMetadataSourceLoader {

    private final ChatTurnAuditMapper chatTurnAuditMapper;

    public FirstTurnMetadataSourceLoader(ChatTurnAuditMapper chatTurnAuditMapper) {
        this.chatTurnAuditMapper = chatTurnAuditMapper;
    }

    @Override
    public SessionMetadataContext load(String sessionId, String sourceTurnId) {
        ChatTurnAudit turnAudit = chatTurnAuditMapper.selectById(sourceTurnId);
        if (turnAudit == null) {
            throw new IllegalStateException("元数据来源轮次不存在: " + sourceTurnId);
        }

        SessionMetadataContext context = new SessionMetadataContext();
        context.setSessionId(sessionId);
        context.setSourceTurnId(sourceTurnId);
        context.setUserInput(turnAudit.getUserInput());
        return context;
    }
}

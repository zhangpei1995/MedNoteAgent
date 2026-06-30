package org.med.note.agent.title;

import org.med.note.dao.ChatTurnAuditMapper;
import org.med.note.domain.entity.ChatTurnAudit;
import org.springframework.stereotype.Component;

/**
 * 使用首轮用户输入作为标题生成来源。
 */
@Component
public class FirstTurnTitleSourceLoader implements SessionTitleSourceLoader {

    private final ChatTurnAuditMapper chatTurnAuditMapper;

    public FirstTurnTitleSourceLoader(ChatTurnAuditMapper chatTurnAuditMapper) {
        this.chatTurnAuditMapper = chatTurnAuditMapper;
    }

    @Override
    public String loadUserInput(String sourceTurnId) {
        ChatTurnAudit turnAudit = chatTurnAuditMapper.selectById(sourceTurnId);
        if (turnAudit == null) {
            throw new IllegalStateException("标题来源轮次不存在: " + sourceTurnId);
        }
        return turnAudit.getUserInput();
    }
}

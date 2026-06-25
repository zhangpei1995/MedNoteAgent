package org.med.note.agent.runtime;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.med.note.agent.execution.AgentConversationMessage;
import org.med.note.agent.execution.ConversationMessageLoader;
import org.med.note.dao.ChatTurnAuditMapper;
import org.med.note.domain.entity.ChatTurnAudit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于轮次审计表加载完整会话历史的上下文策略。
 *
 * <p>当前阶段不做 token 裁剪或摘要优化，只按会话内创建时间正序还原用户和助手消息。
 * 未完成轮次没有助手输出时，仅保留用户输入，确保继续对话时当前轮问题进入模型上下文。</p>
 */
@Component
public class FullConversationMessageLoader implements ConversationMessageLoader {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final ChatTurnAuditMapper chatTurnAuditMapper;

    public FullConversationMessageLoader(ChatTurnAuditMapper chatTurnAuditMapper) {
        this.chatTurnAuditMapper = chatTurnAuditMapper;
    }

    @Override
    public List<AgentConversationMessage> loadMessages(String turnId) {
        ChatTurnAudit currentTurn = chatTurnAuditMapper.selectById(turnId);
        if (currentTurn == null) {
            throw new IllegalStateException("对话轮次不存在: " + turnId);
        }

        List<ChatTurnAudit> turns = chatTurnAuditMapper.selectList(new LambdaQueryWrapper<ChatTurnAudit>()
                .eq(ChatTurnAudit::getSessionId, currentTurn.getSessionId())
                .orderByAsc(ChatTurnAudit::getCreatedAt)
                .orderByAsc(ChatTurnAudit::getId));

        List<AgentConversationMessage> messages = new ArrayList<>();
        for (ChatTurnAudit turn : turns) {
            if (StrUtil.isNotBlank(turn.getUserInput())) {
                messages.add(AgentConversationMessage.of(ROLE_USER, turn.getUserInput()));
            }
            if (StrUtil.isNotBlank(turn.getAssistantOutput())) {
                messages.add(AgentConversationMessage.of(ROLE_ASSISTANT, turn.getAssistantOutput()));
            }
        }
        return messages;
    }
}

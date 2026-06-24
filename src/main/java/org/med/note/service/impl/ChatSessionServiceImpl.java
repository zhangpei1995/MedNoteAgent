package org.med.note.service.impl;

import org.med.note.agent.runtime.ChatTurnSubmissionRuntime;
import org.med.note.dao.ChatSessionMapper;
import org.med.note.dao.ChatTurnAuditMapper;
import org.med.note.domain.entity.ChatSession;
import org.med.note.domain.entity.ChatTurnAudit;
import org.med.note.dto.ChatTurnStatusResponse;
import org.med.note.dto.SubmitChatTurnRequest;
import org.med.note.dto.SubmitChatTurnResponse;
import org.med.note.service.spi.ChatSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 基于 SQLite 和 MyBatis Plus 的会话服务实现。
 *
 * <p>当前实现作为 Controller 面向会话能力的服务契约入口，
 * 将聊天提交编排委托给 Agent runtime，并保留轮次状态查询能力。</p>
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatTurnAuditMapper chatTurnAuditMapper;
    private final ChatTurnSubmissionRuntime chatTurnSubmissionRuntime;

    public ChatSessionServiceImpl(
            ChatSessionMapper chatSessionMapper,
            ChatTurnAuditMapper chatTurnAuditMapper,
            ChatTurnSubmissionRuntime chatTurnSubmissionRuntime
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatTurnAuditMapper = chatTurnAuditMapper;
        this.chatTurnSubmissionRuntime = chatTurnSubmissionRuntime;
    }

    @Override
    public SubmitChatTurnResponse submitTurn(SubmitChatTurnRequest request) {
        return SubmitChatTurnResponse.of(chatTurnSubmissionRuntime.submit(request));
    }

    @Override
    public ChatTurnStatusResponse getTurnStatus(String turnId) {
        ChatTurnAudit turnAudit = chatTurnAuditMapper.selectById(turnId);
        if (turnAudit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话轮次不存在");
        }

        ChatSession session = chatSessionMapper.selectById(turnAudit.getSessionId());
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        }

        ChatTurnStatusResponse response = new ChatTurnStatusResponse();
        response.setTurnId(turnAudit.getId());
        response.setSessionId(turnAudit.getSessionId());
        response.setTitle(session.getTitle());
        response.setStatus(turnAudit.getStatus());
        response.setUserInput(turnAudit.getUserInput());
        response.setAssistantOutput(turnAudit.getAssistantOutput());
        response.setErrorMessage(turnAudit.getErrorMessage());
        response.setCreatedAt(turnAudit.getCreatedAt());
        response.setCompletedAt(turnAudit.getCompletedAt());
        return response;
    }
}

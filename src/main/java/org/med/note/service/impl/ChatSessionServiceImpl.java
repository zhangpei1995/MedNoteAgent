package org.med.note.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * 基于 SQLite 和 MyBatis Plus 的会话服务实现。
 *
 * <p>当前实现只创建会话和写入 WAITING_AGENT 状态的轮次审计记录。
 * Agent 接入后，应在同一轮次记录上补充模型请求、响应、输出、耗时和最终状态。</p>
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String SESSION_STATUS_ACTIVE = "ACTIVE";
    private static final String TURN_STATUS_WAITING_AGENT = "WAITING_AGENT";
    private static final String DEFAULT_MODEL_PROVIDER = "dashscope";
    private static final String DEFAULT_MODEL_NAME = "pending-agent";

    private final ChatSessionMapper chatSessionMapper;
    private final ChatTurnAuditMapper chatTurnAuditMapper;

    public ChatSessionServiceImpl(
            ChatSessionMapper chatSessionMapper,
            ChatTurnAuditMapper chatTurnAuditMapper
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatTurnAuditMapper = chatTurnAuditMapper;
    }

    @Override
    @Transactional
    public SubmitChatTurnResponse submitTurn(SubmitChatTurnRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ChatSession session = resolveSession(request, now);

        ChatTurnAudit turnAudit = new ChatTurnAudit();
        turnAudit.setId(IdUtil.fastSimpleUUID());
        turnAudit.setSessionId(session.getId());
        turnAudit.setUserInput(request.getUserInput());
        turnAudit.setModelProvider(DEFAULT_MODEL_PROVIDER);
        turnAudit.setModelName(DEFAULT_MODEL_NAME);
        turnAudit.setStatus(TURN_STATUS_WAITING_AGENT);
        turnAudit.setCreatedAt(now);
        chatTurnAuditMapper.insert(turnAudit);

        SubmitChatTurnResponse response = new SubmitChatTurnResponse();
        response.setSessionId(session.getId());
        response.setTurnId(turnAudit.getId());
        response.setTitle(session.getTitle());
        response.setTurnStatus(turnAudit.getStatus());
        response.setCreatedAt(turnAudit.getCreatedAt());
        return response;
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

    /**
     * 根据请求中的 sessionId 决定创建新会话或复用已有会话。
     *
     * @param request 用户提交参数，sessionId 为空表示首次提交并创建会话
     * @param now 本次提交的统一业务时间，用于保持 session 和 turn 时间一致
     * @return 可用于写入本轮审计记录的会话实体
     */
    private ChatSession resolveSession(SubmitChatTurnRequest request, LocalDateTime now) {
        if (StrUtil.isBlank(request.getSessionId())) {
            ChatSession session = new ChatSession();
            session.setId(IdUtil.fastSimpleUUID());
            session.setUserId(StrUtil.blankToDefault(request.getUserId(), null));
            session.setTitle(resolveTitle(request));
            session.setStatus(SESSION_STATUS_ACTIVE);
            session.setCreatedAt(now);
            session.setUpdatedAt(now);
            chatSessionMapper.insert(session);
            return session;
        }

        ChatSession session = chatSessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        session.setUpdatedAt(now);
        chatSessionMapper.updateById(session);
        return session;
    }

    /**
     * 生成会话展示标题。
     *
     * @param request 用户提交参数；title 为空时使用用户输入前 30 个字符作为标题
     * @return 会话标题，用于列表和轮次状态查询展示
     */
    private String resolveTitle(SubmitChatTurnRequest request) {
        if (StrUtil.isNotBlank(request.getTitle())) {
            return request.getTitle();
        }
        return StrUtil.maxLength(request.getUserInput(), 30);
    }
}

package org.med.note.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.med.note.agent.runtime.ChatTurnSubmissionRuntime;
import org.med.note.dao.ChatSessionMetadataMapper;
import org.med.note.dao.ChatSessionMapper;
import org.med.note.dao.ChatTurnAuditMapper;
import org.med.note.domain.entity.ChatSession;
import org.med.note.domain.entity.ChatSessionMetadata;
import org.med.note.domain.entity.ChatTurnAudit;
import org.med.note.dto.ChatSessionMetadataResponse;
import org.med.note.dto.ChatSessionSummaryResponse;
import org.med.note.dto.ChatTurnRecordResponse;
import org.med.note.dto.ChatTurnStatusResponse;
import org.med.note.dto.SubmitChatTurnRequest;
import org.med.note.dto.SubmitChatTurnResponse;
import org.med.note.service.spi.ChatSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 SQLite 和 MyBatis Plus 的会话服务实现。
 *
 * <p>当前实现作为 Controller 面向会话能力的服务契约入口，
 * 将聊天提交编排委托给 Agent runtime，并提供前端会话列表、会话历史和轮次状态查询能力。</p>
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionMetadataMapper metadataMapper;
    private final ChatTurnAuditMapper chatTurnAuditMapper;
    private final ChatTurnSubmissionRuntime chatTurnSubmissionRuntime;

    public ChatSessionServiceImpl(
            ChatSessionMapper chatSessionMapper,
            ChatSessionMetadataMapper metadataMapper,
            ChatTurnAuditMapper chatTurnAuditMapper,
            ChatTurnSubmissionRuntime chatTurnSubmissionRuntime
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.metadataMapper = metadataMapper;
        this.chatTurnAuditMapper = chatTurnAuditMapper;
        this.chatTurnSubmissionRuntime = chatTurnSubmissionRuntime;
    }

    @Override
    public SubmitChatTurnResponse submitTurn(SubmitChatTurnRequest request) {
        return SubmitChatTurnResponse.of(chatTurnSubmissionRuntime.submit(request));
    }

    @Override
    public List<ChatSessionSummaryResponse> listSessions(String keyword) {
        String normalizedKeyword = StrUtil.trimToEmpty(keyword);
        if (StrUtil.isBlank(normalizedKeyword)) {
            return listSessionSummaries(new LambdaQueryWrapper<ChatSession>());
        }

        Set<String> matchedSessionIds = new LinkedHashSet<>();
        metadataMapper.selectList(new LambdaQueryWrapper<ChatSessionMetadata>()
                        .like(ChatSessionMetadata::getTitle, normalizedKeyword)
                        .or()
                        .like(ChatSessionMetadata::getConsultationCategory, normalizedKeyword)
                        .or()
                        .like(ChatSessionMetadata::getRecognizedDrugName, normalizedKeyword)
                        .or()
                        .like(ChatSessionMetadata::getInstructionItem, normalizedKeyword)
                        .or()
                        .like(ChatSessionMetadata::getUnderstandingText, normalizedKeyword))
                .forEach(metadata -> matchedSessionIds.add(metadata.getSessionId()));
        chatTurnAuditMapper.selectList(new LambdaQueryWrapper<ChatTurnAudit>()
                        .like(ChatTurnAudit::getUserInput, normalizedKeyword)
                        .or()
                        .like(ChatTurnAudit::getAssistantOutput, normalizedKeyword))
                .forEach(turnAudit -> matchedSessionIds.add(turnAudit.getSessionId()));

        if (matchedSessionIds.isEmpty()) {
            return List.of();
        }

        return listSessionSummaries(new LambdaQueryWrapper<ChatSession>()
                .in(ChatSession::getId, matchedSessionIds));
    }

    private List<ChatSessionSummaryResponse> listSessionSummaries(LambdaQueryWrapper<ChatSession> wrapper) {
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper
                        .orderByDesc(ChatSession::getUpdatedAt)
                        .orderByDesc(ChatSession::getCreatedAt));
        Map<String, ChatSessionMetadata> metadataMap = loadMetadataMap(sessions);
        return sessions.stream()
                .map(session -> ChatSessionSummaryResponse.of(session, metadataMap.get(session.getId())))
                .toList();
    }

    @Override
    public ChatSessionMetadataResponse getSessionMetadata(String sessionId) {
        ensureSessionExists(sessionId);
        ChatSessionMetadata metadata = metadataMapper.selectOne(new LambdaQueryWrapper<ChatSessionMetadata>()
                .eq(ChatSessionMetadata::getSessionId, sessionId)
                .last("LIMIT 1"));
        return ChatSessionMetadataResponse.of(sessionId, metadata);
    }

    @Override
    public List<ChatTurnRecordResponse> listSessionTurns(String sessionId) {
        ensureSessionExists(sessionId);
        return chatTurnAuditMapper.selectList(new LambdaQueryWrapper<ChatTurnAudit>()
                        .eq(ChatTurnAudit::getSessionId, sessionId)
                        .orderByAsc(ChatTurnAudit::getCreatedAt))
                .stream()
                .map(ChatTurnRecordResponse::of)
                .toList();
    }

    @Override
    public ChatTurnStatusResponse getTurnStatus(String turnId) {
        ChatTurnAudit turnAudit = chatTurnAuditMapper.selectById(turnId);
        if (turnAudit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话轮次不存在");
        }

        ChatTurnStatusResponse response = new ChatTurnStatusResponse();
        response.setTurnId(turnAudit.getId());
        response.setSessionId(turnAudit.getSessionId());
        response.setStatus(turnAudit.getStatus());
        response.setUserInput(turnAudit.getUserInput());
        response.setAssistantOutput(turnAudit.getAssistantOutput());
        response.setErrorMessage(turnAudit.getErrorMessage());
        response.setCreatedAt(turnAudit.getCreatedAt());
        response.setCompletedAt(turnAudit.getCompletedAt());
        return response;
    }

    private ChatSession ensureSessionExists(String sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private Map<String, ChatSessionMetadata> loadMetadataMap(List<ChatSession> sessions) {
        if (sessions.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> sessionIds = sessions.stream()
                .map(ChatSession::getId)
                .toList();
        return metadataMapper.selectList(new LambdaQueryWrapper<ChatSessionMetadata>()
                        .in(ChatSessionMetadata::getSessionId, sessionIds))
                .stream()
                .collect(Collectors.toMap(ChatSessionMetadata::getSessionId, Function.identity(), (left, right) -> left));
    }
}

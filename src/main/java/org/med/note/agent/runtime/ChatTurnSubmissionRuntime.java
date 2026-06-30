package org.med.note.agent.runtime;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.med.note.agent.client.ChatAgentClient;
import org.med.note.agent.lifecycle.ChatTurnLifecycleManager;
import org.med.note.agent.title.SessionTitleGenerationClient;
import org.med.note.agent.title.SessionTitleLifecycleManager;
import org.med.note.dao.ChatSessionMapper;
import org.med.note.domain.entity.ChatSession;
import org.med.note.domain.entity.ChatTurnAudit;
import org.med.note.dto.SubmitChatTurnRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * 编排一轮聊天提交进入 Agent 执行队列的运行时组件。
 *
 * <p>该组件负责提交用例的正式闭环：创建或复用会话、创建等待执行的轮次审计记录，
 * 并在事务提交后触发 Agent 异步执行。它不负责 HTTP 入参出参，也不执行模型调用本身。</p>
 */
@Component
public class ChatTurnSubmissionRuntime {

    private static final String SESSION_STATUS_ACTIVE = "ACTIVE";

    private final ChatSessionMapper chatSessionMapper;
    private final ChatTurnLifecycleManager chatTurnLifecycleManager;
    private final ChatAgentClient chatAgentClient;
    private final SessionTitleGenerationClient sessionTitleGenerationClient;

    public ChatTurnSubmissionRuntime(
            ChatSessionMapper chatSessionMapper,
            ChatTurnLifecycleManager chatTurnLifecycleManager,
            ChatAgentClient chatAgentClient,
            SessionTitleGenerationClient sessionTitleGenerationClient
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatTurnLifecycleManager = chatTurnLifecycleManager;
        this.chatAgentClient = chatAgentClient;
        this.sessionTitleGenerationClient = sessionTitleGenerationClient;
    }

    /**
     * 提交一轮用户输入，创建审计轮次并安排 Agent 在事务提交后执行。
     *
     * @param request 用户提交参数；sessionId 为空时创建会话，不为空时追加到已有会话
     * @return 已确认的会话和等待 Agent 执行的轮次审计记录
     */
    @Transactional
    public ChatTurnSubmission submit(SubmitChatTurnRequest request) {
        LocalDateTime now = LocalDateTime.now();
        boolean createSession = StrUtil.isBlank(request.getSessionId());
        ChatSession session = resolveSession(request, now);
        ChatTurnAudit turnAudit = chatTurnLifecycleManager.createWaitingTurn(
                session.getId(),
                request.getUserInput(),
                now
        );
        executeAgentAfterCommit(turnAudit.getId());
        if (createSession) {
            generateTitleAfterCommit(session.getId(), turnAudit.getId());
        }

        ChatTurnSubmission submission = new ChatTurnSubmission();
        submission.setSession(session);
        submission.setTurnAudit(turnAudit);
        return submission;
    }

    private void executeAgentAfterCommit(String turnId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chatAgentClient.executeAsync(turnId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatAgentClient.executeAsync(turnId);
            }
        });
    }

    private void generateTitleAfterCommit(String sessionId, String sourceTurnId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sessionTitleGenerationClient.generateAsync(sessionId, sourceTurnId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sessionTitleGenerationClient.generateAsync(sessionId, sourceTurnId);
            }
        });
    }

    private ChatSession resolveSession(SubmitChatTurnRequest request, LocalDateTime now) {
        if (StrUtil.isBlank(request.getSessionId())) {
            ChatSession session = new ChatSession();
            session.setId(IdUtil.fastSimpleUUID());
            session.setUserId(StrUtil.blankToDefault(request.getUserId(), null));
            session.setTitle(null);
            session.setTitleStatus(SessionTitleLifecycleManager.TITLE_STATUS_GENERATING);
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
}

package org.med.note.agent.lifecycle;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.IdUtil;
import org.med.note.agent.execution.AgentExecutionCommand;
import org.med.note.agent.execution.AgentExecutionResult;
import org.med.note.agent.execution.ConversationMessageLoader;
import org.med.note.dao.ChatTurnAuditMapper;
import org.med.note.domain.entity.ChatTurnAudit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 管理对话轮次在 Agent 执行过程中的审计状态流转。
 */
@Component
public class ChatTurnLifecycleManager {

    private static final String TURN_STATUS_WAITING_AGENT = "WAITING_AGENT";
    private static final String TURN_STATUS_PROCESSING = "PROCESSING";
    private static final String TURN_STATUS_SUCCESS = "SUCCESS";
    private static final String TURN_STATUS_FAILED = "FAILED";
    private static final String MODEL_PROVIDER_DASHSCOPE = "dashscope";
    private static final String MODEL_NAME_PENDING_AGENT = "pending-agent";

    private final ChatTurnAuditMapper chatTurnAuditMapper;
    private final ConversationMessageLoader conversationMessageLoader;

    public ChatTurnLifecycleManager(
            ChatTurnAuditMapper chatTurnAuditMapper,
            ConversationMessageLoader conversationMessageLoader
    ) {
        this.chatTurnAuditMapper = chatTurnAuditMapper;
        this.conversationMessageLoader = conversationMessageLoader;
    }

    /**
     * 创建等待 Agent 执行的轮次审计记录。
     *
     * @param sessionId 所属会话 ID
     * @param userInput 用户本轮原始输入，必须原样保存以支持医学安全追溯
     * @param createdAt 轮次创建时间，通常与会话更新时间保持一致
     * @return 已落库的 WAITING_AGENT 轮次审计记录
     */
    @Transactional
    public ChatTurnAudit createWaitingTurn(String sessionId, String userInput, LocalDateTime createdAt) {
        ChatTurnAudit turnAudit = new ChatTurnAudit();
        turnAudit.setId(IdUtil.fastSimpleUUID());
        turnAudit.setSessionId(sessionId);
        turnAudit.setUserInput(userInput);
        turnAudit.setModelProvider(MODEL_PROVIDER_DASHSCOPE);
        turnAudit.setModelName(MODEL_NAME_PENDING_AGENT);
        turnAudit.setStatus(TURN_STATUS_WAITING_AGENT);
        turnAudit.setCreatedAt(createdAt);
        chatTurnAuditMapper.insert(turnAudit);
        return turnAudit;
    }

    /**
     * 将待执行轮次标记为处理中，并返回 Agent 执行命令。
     *
     * @param turnId 对话轮次审计 ID
     * @return 可传递给 Agent 执行策略的命令
     */
    @Transactional
    public AgentExecutionCommand markProcessing(String turnId) {
        ChatTurnAudit turnAudit = requireTurnAudit(turnId);
        turnAudit.setStatus(TURN_STATUS_PROCESSING);
        chatTurnAuditMapper.updateById(turnAudit);

        AgentExecutionCommand command = new AgentExecutionCommand();
        command.setTurnId(turnAudit.getId());
        command.setSessionId(turnAudit.getSessionId());
        command.setUserInput(turnAudit.getUserInput());
        command.setConversationMessages(conversationMessageLoader.loadMessages(turnAudit.getId()));
        return command;
    }

    /**
     * 将轮次标记为执行成功，并写入 Agent 输出和审计信息。
     *
     * @param turnId 对话轮次审计 ID
     * @param result Agent 执行结果
     * @param elapsedMs 本轮执行耗时，单位毫秒
     */
    @Transactional
    public void markSuccess(String turnId, AgentExecutionResult result, long elapsedMs) {
        ChatTurnAudit turnAudit = requireTurnAudit(turnId);
        turnAudit.setAssistantOutput(result.getAssistantOutput());
        turnAudit.setModelProvider(result.getModelProvider());
        turnAudit.setModelName(result.getModelName());
        turnAudit.setSystemPrompt(result.getSystemPrompt());
        turnAudit.setRequestJson(result.getRequestJson());
        turnAudit.setResponseJson(result.getResponseJson());
        turnAudit.setStatus(TURN_STATUS_SUCCESS);
        turnAudit.setErrorMessage(null);
        turnAudit.setElapsedMs(elapsedMs);
        turnAudit.setCompletedAt(LocalDateTime.now());
        chatTurnAuditMapper.updateById(turnAudit);
    }

    /**
     * 将轮次标记为执行失败，并写入可追溯的错误摘要。
     *
     * @param turnId 对话轮次审计 ID
     * @param exception Agent 执行异常
     * @param elapsedMs 本轮执行耗时，单位毫秒
     */
    @Transactional
    public void markFailed(String turnId, Exception exception, long elapsedMs) {
        ChatTurnAudit turnAudit = chatTurnAuditMapper.selectById(turnId);
        if (turnAudit == null) {
            return;
        }

        turnAudit.setStatus(TURN_STATUS_FAILED);
        turnAudit.setErrorMessage(ExceptionUtil.getRootCauseMessage(exception));
        turnAudit.setElapsedMs(elapsedMs);
        turnAudit.setCompletedAt(LocalDateTime.now());
        chatTurnAuditMapper.updateById(turnAudit);
    }

    private ChatTurnAudit requireTurnAudit(String turnId) {
        ChatTurnAudit turnAudit = chatTurnAuditMapper.selectById(turnId);
        if (turnAudit == null) {
            throw new IllegalStateException("对话轮次不存在: " + turnId);
        }
        return turnAudit;
    }
}

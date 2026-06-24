package org.med.note.service.spi;

import org.med.note.dto.ChatTurnStatusResponse;
import org.med.note.dto.SubmitChatTurnRequest;
import org.med.note.dto.SubmitChatTurnResponse;

/**
 * 会话与对话轮次写入服务。
 *
 * <p>该接口承担 Controller 与持久化层之间的业务契约：决定何时创建会话、何时复用会话、
 * 以及一轮用户输入进入 Agent 前应如何形成可审计记录。</p>
 */
public interface ChatSessionService {

    /**
     * 提交一轮用户输入并生成待执行审计记录。
     *
     * @param request 用户输入和可选会话参数；sessionId 为空时创建会话，不为空时追加到已有会话
     * @return 会话 ID、轮次 ID、标题和当前轮次初始状态
     */
    SubmitChatTurnResponse submitTurn(SubmitChatTurnRequest request);

    /**
     * 查询一轮对话的执行状态。
     *
     * @param turnId 对话轮次 ID，由 submitTurn 返回
     * @return 轮次状态、会话标题、用户输入、助手输出和错误信息
     */
    ChatTurnStatusResponse getTurnStatus(String turnId);
}

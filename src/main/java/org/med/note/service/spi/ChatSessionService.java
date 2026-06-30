package org.med.note.service.spi;

import org.med.note.dto.ChatTurnStatusResponse;
import org.med.note.dto.ChatSessionSummaryResponse;
import org.med.note.dto.ChatSessionTitleResponse;
import org.med.note.dto.ChatTurnRecordResponse;
import org.med.note.dto.SubmitChatTurnRequest;
import org.med.note.dto.SubmitChatTurnResponse;

import java.util.List;

/**
 * 会话与对话轮次服务。
 *
 * <p>该接口承担 Controller 与持久化层之间的业务契约：决定何时创建会话、何时复用会话、
 * 一轮用户输入进入 Agent 前应如何形成可审计记录，以及前端如何读取会话历史。</p>
 */
public interface ChatSessionService {

    /**
     * 提交一轮用户输入并生成待执行审计记录。
     *
     * @param request 用户输入和可选会话参数；sessionId 为空时创建会话，不为空时追加到已有会话
     * @return 会话 ID、轮次 ID、标题状态和当前轮次初始状态
     */
    SubmitChatTurnResponse submitTurn(SubmitChatTurnRequest request);

    /**
     * 查询会话列表，按最近更新时间倒序返回。
     *
     * @param keyword 可选搜索关键字；为空时返回全部会话，不为空时匹配会话标题、用户输入和助手输出
     * @return 会话摘要列表；没有会话或没有匹配结果时返回空列表
     */
    List<ChatSessionSummaryResponse> listSessions(String keyword);

    /**
     * 查询指定会话的标题生成状态。
     *
     * @param sessionId 会话 ID，必须对应已存在的会话
     * @return 会话真实标题和标题生成状态；标题未生成时 title 为空
     */
    ChatSessionTitleResponse getSessionTitle(String sessionId);

    /**
     * 查询指定会话下的全部轮次，按创建时间正序返回。
     *
     * @param sessionId 会话 ID，必须对应已存在的会话
     * @return 会话内全部轮次记录；会话存在但还没有轮次时返回空列表
     */
    List<ChatTurnRecordResponse> listSessionTurns(String sessionId);

    /**
     * 查询一轮对话的执行状态。
     *
     * @param turnId 对话轮次 ID，由 submitTurn 返回
     * @return 轮次状态、用户输入、助手输出和错误信息
     */
    ChatTurnStatusResponse getTurnStatus(String turnId);
}

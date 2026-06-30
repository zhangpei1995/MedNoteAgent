package org.med.note.controller;

import jakarta.validation.Valid;
import org.med.note.dto.ChatSessionMetadataResponse;
import org.med.note.dto.ChatSessionSummaryResponse;
import org.med.note.dto.ChatTurnRecordResponse;
import org.med.note.dto.ChatTurnStatusResponse;
import org.med.note.dto.SubmitChatTurnRequest;
import org.med.note.dto.SubmitChatTurnResponse;
import org.med.note.service.spi.ChatSessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话提交、会话历史与轮次状态查询入口。
 *
 * <p>Controller 只暴露会话、元数据和轮次查询入口；提交后的 Agent 执行与元数据生成由后端异步链路处理。</p>
 */
@Validated
@RestController
@RequestMapping("/api/chat")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * 提交一轮用户输入。
     *
     * <p>请求不携带 sessionId 时创建新会话；携带 sessionId 时向已有会话追加一轮记录。
     * 当前返回的 turnId 用于后续查询 Agent 执行状态。</p>
     *
     * @param request 会话提交参数，userInput 必填，sessionId/userId 可选
     * @return 新建或复用后的 sessionId、当前轮次 turnId、元数据状态和初始轮次状态
     */
    @PostMapping("/sessions")
    public SubmitChatTurnResponse submitTurn(@Valid @RequestBody SubmitChatTurnRequest request) {
        return chatSessionService.submitTurn(request);
    }

    /**
     * 查询会话摘要。
     *
     * <p>返回结果按最近更新时间倒序排列，用于前端左侧会话列表。keyword 为空时查询全部会话；
     * keyword 不为空时按会话元数据、用户输入和助手输出搜索历史对话。</p>
     *
     * @param keyword 可选搜索关键字；为空时返回全部会话
     * @return 会话摘要列表；没有会话或没有匹配结果时返回空列表
     */
    @GetMapping("/sessions")
    public List<ChatSessionSummaryResponse> listSessions(@RequestParam(required = false) String keyword) {
        return chatSessionService.listSessions(keyword);
    }

    /**
     * 查询指定会话的元数据生成状态。
     *
     * <p>元数据生成独立于单轮 Agent 执行；metadataStatus 为 GENERATED 或 FAILED 时可停止轮询。</p>
     *
     * @param sessionId 会话 ID
     * @return 会话元数据生成状态和结构化结果
     */
    @GetMapping("/sessions/{sessionId}/metadata")
    public ChatSessionMetadataResponse getSessionMetadata(@PathVariable String sessionId) {
        return chatSessionService.getSessionMetadata(sessionId);
    }

    /**
     * 查询指定会话的全部轮次。
     *
     * <p>返回结果按创建时间正序排列，用于前端切换会话后还原完整对话。</p>
     *
     * @param sessionId 会话 ID
     * @return 会话内全部轮次记录
     */
    @GetMapping("/sessions/{sessionId}/turns")
    public List<ChatTurnRecordResponse> listSessionTurns(@PathVariable String sessionId) {
        return chatSessionService.listSessionTurns(sessionId);
    }

    /**
     * 查询指定轮次的执行状态。
     *
     * <p>用于前端拿到 turnId 后轮询当前轮次状态、用户输入和后续 Agent 输出。</p>
     *
     * @param turnId 提交会话轮次时返回的轮次 ID
     * @return 当前轮次状态、用户输入、助手输出和错误信息
     */
    @GetMapping("/turns/{turnId}")
    public ChatTurnStatusResponse getTurnStatus(@PathVariable String turnId) {
        return chatSessionService.getTurnStatus(turnId);
    }
}

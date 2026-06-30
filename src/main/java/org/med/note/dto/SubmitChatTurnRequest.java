package org.med.note.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交一轮聊天输入的请求参数。
 *
 * <p>首次发起咨询时不传 sessionId，服务会创建新会话并异步生成会话元数据；
 * 继续同一咨询时传入已有 sessionId。当前阶段只记录用户输入并生成待 Agent 执行的审计轮次。</p>
 */
@Data
public class SubmitChatTurnRequest {

    /**
     * 已有会话 ID。为空表示创建新会话；不为空表示向该会话追加一轮输入。
     */
    private String sessionId;

    /**
     * 外部用户 ID。当前没有独立用户表，可为空；传入时写入新建会话用于后续聚合查询。
     */
    private String userId;

    /**
     * 用户本轮原始输入，必须保留原文以满足医学问答审计和后续 Agent 执行。
     */
    @NotBlank(message = "用户输入不能为空")
    private String userInput;
}

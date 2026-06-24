package org.med.note.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单轮对话审计持久化实体。
 *
 * <p>一条记录对应用户提交的一轮输入及其后续 Agent 执行结果。医学问答链路中的模型请求、
 * 模型响应、最终输出、异常和耗时都应沉淀在本表，便于追溯和评测。</p>
 */
@Data
@TableName("chat_turn_audit")
public class ChatTurnAudit {

    /**
     * 轮次审计主键，由服务创建。
     */
    @TableId
    private String id;

    /**
     * 所属会话 ID。
     */
    private String sessionId;

    /**
     * 用户本轮原始输入。
     */
    private String userInput;

    /**
     * Agent 或系统最终输出。Agent 未完成前为空。
     */
    private String assistantOutput;

    /**
     * 模型提供方，例如 dashscope；Agent 未接入前使用占位值。
     */
    private String modelProvider;

    /**
     * 模型名称；Agent 未接入前使用占位值。
     */
    private String modelName;

    /**
     * 本轮实际使用的系统提示词。Agent 未执行前为空。
     */
    private String systemPrompt;

    /**
     * 本轮发给模型或 Agent 的完整请求 JSON。Agent 未执行前为空。
     */
    private String requestJson;

    /**
     * 本轮模型或 Agent 返回的完整响应 JSON。Agent 未执行前为空。
     */
    private String responseJson;

    /**
     * 本轮执行状态，例如 WAITING_AGENT、PROCESSING、SUCCESS、FAILED。
     */
    private String status;

    /**
     * 执行失败时的错误信息。
     */
    private String errorMessage;

    /**
     * Agent 或模型调用耗时，单位毫秒。
     */
    private Long elapsedMs;

    /**
     * 本轮记录创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 本轮执行完成时间。
     */
    private LocalDateTime completedAt;
}

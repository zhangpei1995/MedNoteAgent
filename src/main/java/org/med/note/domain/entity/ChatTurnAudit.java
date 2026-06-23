package org.med.note.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("chat_turn_audit")
public class ChatTurnAudit {

    @TableId
    private String id;

    private String sessionId;

    private String userInput;

    private String assistantOutput;

    private String modelProvider;

    private String modelName;

    private String systemPrompt;

    private String requestJson;

    private String responseJson;

    private String status;

    private String errorMessage;

    private Long elapsedMs;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}

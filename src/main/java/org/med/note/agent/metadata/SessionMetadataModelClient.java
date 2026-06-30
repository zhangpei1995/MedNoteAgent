package org.med.note.agent.metadata;

import com.alibaba.dashscope.common.Message;

import java.util.List;

/**
 * 会话元数据分析模型调用客户端。
 */
public interface SessionMetadataModelClient {

    /**
     * 调用模型并返回原始文本输出。
     *
     * @param messages 模型消息，不应为空
     * @return 模型原始输出文本
     */
    String call(List<Message> messages);
}

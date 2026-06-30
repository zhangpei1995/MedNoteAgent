package org.med.note.agent.metadata;

import com.alibaba.dashscope.common.Message;
import org.med.note.domain.metadata.SessionMetadataFragment;
import org.med.note.domain.metadata.SessionMetadataItem;
import org.med.note.domain.metadata.SessionMetadataResult;

import java.util.List;

/**
 * 单个会话元数据分析项。
 *
 * <p>每个实现类拥有独立 Prompt、输出解析和清洗规则；异步状态、模型调用和落库由统一链路负责。</p>
 *
 * @param <R> 当前分析项输出片段类型
 */
public interface SessionMetadataAnalyzer<R extends SessionMetadataFragment> {

    /**
     * 当前分析项标识。
     */
    SessionMetadataItem item();

    /**
     * 分析项执行顺序；数值越小越先执行。
     */
    int order();

    /**
     * 判断当前分析项是否需要执行。
     *
     * @param context 当前会话分析上下文
     * @param currentResult 已完成分析项合并后的结果
     * @return true 表示需要执行
     */
    boolean supports(SessionMetadataContext context, SessionMetadataResult currentResult);

    /**
     * 构建当前分析项自己的模型消息。
     */
    List<Message> buildMessages(SessionMetadataContext context, SessionMetadataResult currentResult);

    /**
     * 解析模型原始输出并返回强类型片段。
     */
    R parse(String rawOutput, SessionMetadataContext context, SessionMetadataResult currentResult);
}

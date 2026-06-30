package org.med.note.agent.title;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import org.med.note.dao.ChatSessionMapper;
import org.med.note.domain.entity.ChatSession;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 管理会话标题异步生成状态。
 *
 * <p>标题生成是会话元数据更新，不影响对话轮次审计状态；失败时保留空标题，
 * 前端继续展示默认标题。</p>
 */
@Component
public class SessionTitleLifecycleManager {

    public static final String TITLE_STATUS_GENERATING = "GENERATING";
    public static final String TITLE_STATUS_GENERATED = "GENERATED";
    public static final String TITLE_STATUS_FAILED = "FAILED";

    private final ChatSessionMapper chatSessionMapper;

    public SessionTitleLifecycleManager(ChatSessionMapper chatSessionMapper) {
        this.chatSessionMapper = chatSessionMapper;
    }

    /**
     * 标记标题进入生成中状态。
     *
     * @param sessionId 会话 ID
     */
    @Transactional
    public void markGenerating(String sessionId) {
        ChatSession session = requireSession(sessionId);
        session.setTitleStatus(TITLE_STATUS_GENERATING);
        session.setTitleGeneratedAt(null);
        session.setTitleErrorMessage(null);
        chatSessionMapper.updateById(session);
    }

    /**
     * 写入生成成功的真实标题。
     *
     * @param sessionId 会话 ID
     * @param title 模型生成并清洗后的标题
     */
    @Transactional
    public void markGenerated(String sessionId, String title) {
        ChatSession session = requireSession(sessionId);
        session.setTitle(StrUtil.blankToDefault(title, null));
        session.setTitleStatus(TITLE_STATUS_GENERATED);
        session.setTitleGeneratedAt(LocalDateTime.now());
        session.setTitleErrorMessage(null);
        chatSessionMapper.updateById(session);
    }

    /**
     * 记录标题生成失败状态。
     *
     * @param sessionId 会话 ID
     * @param exception 标题生成异常
     */
    @Transactional
    public void markFailed(String sessionId, Exception exception) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }

        session.setTitle(null);
        session.setTitleStatus(TITLE_STATUS_FAILED);
        session.setTitleGeneratedAt(null);
        session.setTitleErrorMessage(ExceptionUtil.getRootCauseMessage(exception));
        chatSessionMapper.updateById(session);
    }

    private ChatSession requireSession(String sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalStateException("会话不存在: " + sessionId);
        }
        return session;
    }
}

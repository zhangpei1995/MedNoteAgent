package org.med.note.agent.title;

/**
 * 会话标题生成来源加载器。
 *
 * <p>当前标题只使用首轮用户输入，后续如需引入更多上下文，应通过该契约扩展来源加载方式。</p>
 */
public interface SessionTitleSourceLoader {

    /**
     * 读取标题生成所需的用户输入。
     *
     * @param sourceTurnId 标题来源轮次 ID
     * @return 用户原始输入
     */
    String loadUserInput(String sourceTurnId);
}

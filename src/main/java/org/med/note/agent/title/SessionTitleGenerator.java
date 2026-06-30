package org.med.note.agent.title;

/**
 * 会话标题生成策略。
 *
 * <p>实现类应只根据用户问题生成短标题，不输出医学结论、诊断或治疗建议。</p>
 */
public interface SessionTitleGenerator {

    /**
     * 根据首轮用户输入生成会话标题。
     *
     * @param userInput 首轮用户原始输入，不应为空
     * @return 可直接展示的短标题
     */
    String generate(String userInput);
}

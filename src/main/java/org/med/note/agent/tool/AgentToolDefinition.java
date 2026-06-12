package org.med.note.agent.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个可由 Agent 动态发现、剪枝和按需调用的工具。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentToolDefinition {
    String name();

    String description();

    String phase();

    int order();

    boolean required() default false;

    /**
     * 用于工具召回的小模型关键词提示；比 triggers 更偏向语义标签。
     */
    String[] keywordHints() default {};

    String[] triggers() default {};
}

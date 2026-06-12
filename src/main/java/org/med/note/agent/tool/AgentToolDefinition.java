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
     * Tool names that must complete before this tool can be selected.
     */
    String[] dependsOn() default {};

    /**
     * Whether the tool can run concurrently with other ready tools against the same context snapshot.
     */
    boolean parallelizable() default true;

    /**
     * 用于工具召回的小模型关键词提示；比 triggers 更偏向语义标签。
     */
    String[] keywordHints() default {};

    String[] triggers() default {};
}

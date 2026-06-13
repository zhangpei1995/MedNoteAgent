package org.med.note.agent.answer;

/**
 * Generates the final answer from internal agent context.
 */
public interface AnswerGenerator {
    String generate(AnswerGenerationContext context);
}

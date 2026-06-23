package org.med.note.llm;

import io.github.cdimascio.dotenv.Dotenv;

public final class QwenProperties {
    private static final String ENV_DIRECTORY = "/Users/zhangpei/IdeaProjects/MedNoteAgent";
    private static final String API_KEY_NAME = "QwenApiKey";
    private static final String DEFAULT_MODEL_NAME = "DefaultModel";
    private static final String FALLBACK_MODEL = "qwen3.7-plus";
    private static final Dotenv DOTENV = Dotenv.configure()
            .directory(ENV_DIRECTORY)
            .filename(".env")
            .ignoreIfMissing()
            .load();

    private QwenProperties() {
    }

    public static String getApiKey() {
        return DOTENV.get(API_KEY_NAME);
    }

    public static String getDefaultModel() {
        return resolveDefaultModel(DOTENV.get(DEFAULT_MODEL_NAME));
    }

    private static String resolveDefaultModel(String configuredModel) {
        if (configuredModel == null || configuredModel.isBlank()) {
            return FALLBACK_MODEL;
        }
        return configuredModel;
    }
}

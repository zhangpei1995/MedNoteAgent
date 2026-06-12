package org.med.note.config;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvConfig {

    private static final Dotenv DOTENV = Dotenv.load();

    public static String getQwenApiKey() {
        return DOTENV.get("QWEN_API_KEY");
    }
}

package org.med.note.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 补齐聊天会话元数据表的轻量 SQLite schema。
 *
 * <p>项目当前使用 Spring SQL init 创建新库；已有本地 SQLite 库不会因
 * schema-sqlite.sql 变化自动创建新表，因此这里保证元数据表存在。</p>
 */
@Component
public class ChatSessionSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createMetadataTable();
    }

    private void createMetadataTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_session_metadata (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL UNIQUE,
                    source_turn_id TEXT,
                    status TEXT NOT NULL,
                    title TEXT,
                    consultation_category TEXT,
                    recognized_drug_name TEXT,
                    instruction_item TEXT,
                    knowledge_status TEXT,
                    scope_status TEXT,
                    understanding_text TEXT,
                    metadata_json TEXT,
                    error_message TEXT,
                    generated_at DATETIME,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    FOREIGN KEY (session_id) REFERENCES chat_session (id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_session_metadata_session_id
                    ON chat_session_metadata (session_id)
                """);
    }
}

package org.med.note.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 补齐聊天会话表的轻量 SQLite schema 字段。
 *
 * <p>项目当前使用 Spring SQL init 创建新库；已有本地 SQLite 库不会因
 * CREATE TABLE IF NOT EXISTS 自动增加新列，因此这里仅补齐本阶段新增的标题元数据列。</p>
 */
@Component
public class ChatSessionSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> columns = jdbcTemplate.queryForList("PRAGMA table_info(chat_session)")
                .stream()
                .map(column -> String.valueOf(column.get("name")).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        addColumnIfMissing(columns, "title_status", "ALTER TABLE chat_session ADD COLUMN title_status TEXT NOT NULL DEFAULT 'GENERATING'");
        addColumnIfMissing(columns, "title_generated_at", "ALTER TABLE chat_session ADD COLUMN title_generated_at DATETIME");
        addColumnIfMissing(columns, "title_error_message", "ALTER TABLE chat_session ADD COLUMN title_error_message TEXT");
        jdbcTemplate.update("""
                UPDATE chat_session
                SET title_status = 'GENERATED',
                    title_generated_at = COALESCE(title_generated_at, updated_at)
                WHERE title IS NOT NULL
                  AND trim(title) <> ''
                  AND title_status = 'GENERATING'
                """);
    }

    private void addColumnIfMissing(Set<String> columns, String columnName, String ddl) {
        if (columns.contains(columnName)) {
            return;
        }

        jdbcTemplate.execute(ddl);
        columns.add(columnName);
    }
}

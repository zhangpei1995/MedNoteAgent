package org.med.note.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 初始化 SQLite 连接的锁等待与日志模式。
 *
 * <p>本项目当前使用 SQLite 作为本地底层存储；该初始化器在 schema 补齐前执行，
 * 确保启动后连接进入 WAL 模式，并在遇到短暂写锁时等待而不是立即抛出 SQLITE_BUSY。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SqliteConnectionInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 SQLite 连接初始化器。
     *
     * @param jdbcTemplate Spring 管理的数据库访问入口，用于执行连接级 PRAGMA
     */
    public SqliteConnectionInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用 SQLite 运行期 PRAGMA。
     *
     * @param args Spring Boot 启动参数，本初始化器不读取该参数
     */
    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("PRAGMA busy_timeout = 10000");
        jdbcTemplate.execute("PRAGMA journal_mode = WAL");
        jdbcTemplate.execute("PRAGMA synchronous = NORMAL");
    }
}

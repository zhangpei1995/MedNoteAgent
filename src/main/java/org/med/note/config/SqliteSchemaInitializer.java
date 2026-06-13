package org.med.note.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the local SQLite schema used by MyBatis-Plus mappers.
 */
@Component
public class SqliteSchemaInitializer {

    private final DataSource dataSource;
    private final String sqlitePath;

    public SqliteSchemaInitializer(
            DataSource dataSource,
            @Value("${mednote.agent.store.sqlite.path:data/mednote-agent.db}") String sqlitePath
    ) {
        this.dataSource = dataSource;
        this.sqlitePath = sqlitePath;
    }

    @PostConstruct
    public void initialize() {
        ensureDatabaseDirectory();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            createAgentTables(statement);
            createKnowledgeGraphTables(statement);
        } catch (SQLException error) {
            throw new IllegalStateException("初始化 SQLite 表结构失败", error);
        }
    }

    private void ensureDatabaseDirectory() {
        Path parent = Path.of(sqlitePath).toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException error) {
            throw new IllegalStateException("创建 SQLite 存储目录失败: " + parent, error);
        }
    }

    private void createAgentTables(Statement statement) throws SQLException {
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agent_runs (
                    session_id VARCHAR(64) PRIMARY KEY,
                    started_at VARCHAR(40) NOT NULL,
                    finished_at VARCHAR(40) NOT NULL,
                    tool_call_count INTEGER NOT NULL,
                    step_count INTEGER NOT NULL,
                    payload_json TEXT NOT NULL,
                    created_at VARCHAR(40) NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agent_steps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id VARCHAR(64) NOT NULL,
                    step_order INTEGER NOT NULL,
                    stage VARCHAR(120) NOT NULL,
                    event_type VARCHAR(40) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    content TEXT,
                    metadata_json TEXT NOT NULL,
                    created_at VARCHAR(40) NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES agent_runs(session_id)
                )
                """);
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agent_tool_calls (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id VARCHAR(64) NOT NULL,
                    tool_order INTEGER NOT NULL,
                    tool_name VARCHAR(120) NOT NULL,
                    phase VARCHAR(80) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    started_at VARCHAR(40) NOT NULL,
                    finished_at VARCHAR(40) NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    summary TEXT,
                    input_snapshot_json TEXT NOT NULL,
                    output_metadata_json TEXT NOT NULL,
                    failure_type VARCHAR(80) NOT NULL,
                    error_type VARCHAR(160),
                    error_message TEXT,
                    payload_json TEXT NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES agent_runs(session_id)
                )
                """);
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_agent_runs_finished_at ON agent_runs(finished_at)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_agent_tool_calls_status_finished_at ON agent_tool_calls(status, finished_at)");
    }

    private void createKnowledgeGraphTables(Statement statement) throws SQLException {
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS knowledge_graph_nodes (
                    node_id VARCHAR(128) PRIMARY KEY,
                    node_type VARCHAR(80) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    canonical_name VARCHAR(255) NOT NULL,
                    source_id VARCHAR(128) NOT NULL,
                    properties_json TEXT NOT NULL,
                    created_at VARCHAR(40) NOT NULL,
                    updated_at VARCHAR(40) NOT NULL
                )
                """);
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS knowledge_graph_edges (
                    edge_id VARCHAR(192) PRIMARY KEY,
                    source_node_id VARCHAR(128) NOT NULL,
                    target_node_id VARCHAR(128) NOT NULL,
                    edge_type VARCHAR(80) NOT NULL,
                    weight REAL NOT NULL,
                    evidence_id VARCHAR(128) NOT NULL,
                    properties_json TEXT NOT NULL,
                    created_at VARCHAR(40) NOT NULL,
                    updated_at VARCHAR(40) NOT NULL,
                    FOREIGN KEY(source_node_id) REFERENCES knowledge_graph_nodes(node_id),
                    FOREIGN KEY(target_node_id) REFERENCES knowledge_graph_nodes(node_id)
                )
                """);
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_nodes_type_name ON knowledge_graph_nodes(node_type, canonical_name)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_edges_source_type ON knowledge_graph_edges(source_node_id, edge_type)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_edges_target_type ON knowledge_graph_edges(target_node_id, edge_type)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_edges_evidence ON knowledge_graph_edges(evidence_id)");
    }
}

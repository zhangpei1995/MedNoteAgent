package org.med.note.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
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
            statement.executeUpdate("PRAGMA foreign_keys = OFF");
            migrateAgentTablesIfNeeded(connection, statement);
            migrateKnowledgeGraphTablesIfNeeded(connection, statement);
            createAgentTables(statement);
            createKnowledgeGraphTables(statement);
            statement.executeUpdate("PRAGMA foreign_keys = ON");
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
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id VARCHAR(64) NOT NULL UNIQUE,
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
                    agent_run_id INTEGER NOT NULL,
                    step_order INTEGER NOT NULL,
                    stage VARCHAR(120) NOT NULL,
                    event_type VARCHAR(40) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    content TEXT,
                    metadata_json TEXT NOT NULL,
                    created_at VARCHAR(40) NOT NULL,
                    FOREIGN KEY(agent_run_id) REFERENCES agent_runs(id)
                )
                """);
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agent_tool_calls (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    agent_run_id INTEGER NOT NULL,
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
                    FOREIGN KEY(agent_run_id) REFERENCES agent_runs(id)
                )
                """);
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_agent_runs_finished_at ON agent_runs(finished_at)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_agent_tool_calls_status_finished_at ON agent_tool_calls(status, finished_at)");
    }

    private void createKnowledgeGraphTables(Statement statement) throws SQLException {
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS knowledge_graph_nodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    node_id VARCHAR(128) NOT NULL UNIQUE,
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
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_node_row_id INTEGER NOT NULL,
                    target_node_row_id INTEGER NOT NULL,
                    edge_type VARCHAR(80) NOT NULL,
                    weight REAL NOT NULL,
                    evidence_id VARCHAR(128) NOT NULL,
                    properties_json TEXT NOT NULL,
                    created_at VARCHAR(40) NOT NULL,
                    updated_at VARCHAR(40) NOT NULL,
                    FOREIGN KEY(source_node_row_id) REFERENCES knowledge_graph_nodes(id),
                    FOREIGN KEY(target_node_row_id) REFERENCES knowledge_graph_nodes(id)
                )
                """);
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_nodes_type_name ON knowledge_graph_nodes(node_type, canonical_name)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_edges_source_type ON knowledge_graph_edges(source_node_row_id, edge_type)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_edges_target_type ON knowledge_graph_edges(target_node_row_id, edge_type)");
        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kg_edges_evidence ON knowledge_graph_edges(evidence_id)");
        statement.executeUpdate("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_kg_edges_relation_evidence
                ON knowledge_graph_edges(source_node_row_id, target_node_row_id, edge_type, evidence_id)
                """);
    }

    private void migrateAgentTablesIfNeeded(Connection connection, Statement statement) throws SQLException {
        if (!tableExists(connection, "agent_runs")) {
            return;
        }
        boolean needsMigration = !columnExists(connection, "agent_runs", "id")
                || tableExists(connection, "agent_steps") && !columnExists(connection, "agent_steps", "agent_run_id")
                || tableExists(connection, "agent_tool_calls") && !columnExists(connection, "agent_tool_calls", "agent_run_id");
        if (!needsMigration) {
            return;
        }

        dropAgentIndexes(statement);
        dropLegacyTables(statement, "agent_tool_calls_legacy", "agent_steps_legacy", "agent_runs_legacy");
        statement.executeUpdate("ALTER TABLE agent_runs RENAME TO agent_runs_legacy");
        if (tableExists(connection, "agent_steps")) {
            statement.executeUpdate("ALTER TABLE agent_steps RENAME TO agent_steps_legacy");
        }
        if (tableExists(connection, "agent_tool_calls")) {
            statement.executeUpdate("ALTER TABLE agent_tool_calls RENAME TO agent_tool_calls_legacy");
        }

        createAgentTables(statement);
        statement.executeUpdate("""
                INSERT INTO agent_runs(session_id, started_at, finished_at, tool_call_count, step_count, payload_json, created_at)
                SELECT session_id, started_at, finished_at, tool_call_count, step_count, payload_json, created_at
                FROM agent_runs_legacy
                """);
        if (tableExists(connection, "agent_steps_legacy")) {
            statement.executeUpdate("""
                    INSERT INTO agent_steps(agent_run_id, step_order, stage, event_type, status, content, metadata_json, created_at)
                    SELECT runs.id, steps.step_order, steps.stage, steps.event_type, steps.status, steps.content,
                           steps.metadata_json, steps.created_at
                    FROM agent_steps_legacy steps
                    JOIN agent_runs runs ON runs.session_id = steps.session_id
                    """);
        }
        if (tableExists(connection, "agent_tool_calls_legacy")) {
            statement.executeUpdate("""
                    INSERT INTO agent_tool_calls(agent_run_id, tool_order, tool_name, phase, status, started_at, finished_at,
                                                 duration_ms, summary, input_snapshot_json, output_metadata_json,
                                                 failure_type, error_type, error_message, payload_json)
                    SELECT runs.id, calls.tool_order, calls.tool_name, calls.phase, calls.status, calls.started_at,
                           calls.finished_at, calls.duration_ms, calls.summary, calls.input_snapshot_json,
                           calls.output_metadata_json, calls.failure_type, calls.error_type, calls.error_message,
                           calls.payload_json
                    FROM agent_tool_calls_legacy calls
                    JOIN agent_runs runs ON runs.session_id = calls.session_id
                    """);
        }
        dropLegacyTables(statement, "agent_tool_calls_legacy", "agent_steps_legacy", "agent_runs_legacy");
    }

    private void migrateKnowledgeGraphTablesIfNeeded(Connection connection, Statement statement) throws SQLException {
        if (!tableExists(connection, "knowledge_graph_nodes")) {
            return;
        }
        boolean needsMigration = !columnExists(connection, "knowledge_graph_nodes", "id")
                || tableExists(connection, "knowledge_graph_edges")
                && (!columnExists(connection, "knowledge_graph_edges", "source_node_row_id")
                || columnExists(connection, "knowledge_graph_edges", "edge_id"));
        if (!needsMigration) {
            return;
        }

        dropKnowledgeGraphIndexes(statement);
        dropLegacyTables(statement, "knowledge_graph_edges_legacy", "knowledge_graph_nodes_legacy");
        statement.executeUpdate("ALTER TABLE knowledge_graph_nodes RENAME TO knowledge_graph_nodes_legacy");
        if (tableExists(connection, "knowledge_graph_edges")) {
            statement.executeUpdate("ALTER TABLE knowledge_graph_edges RENAME TO knowledge_graph_edges_legacy");
        }

        createKnowledgeGraphTables(statement);
        statement.executeUpdate("""
                INSERT INTO knowledge_graph_nodes(node_id, node_type, name, canonical_name, source_id,
                                                  properties_json, created_at, updated_at)
                SELECT node_id, node_type, name, canonical_name, source_id, properties_json, created_at, updated_at
                FROM knowledge_graph_nodes_legacy
                """);
        if (tableExists(connection, "knowledge_graph_edges_legacy")) {
            if (columnExists(connection, "knowledge_graph_edges_legacy", "source_node_row_id")) {
                statement.executeUpdate("""
                        INSERT INTO knowledge_graph_edges(source_node_row_id, target_node_row_id, edge_type, weight,
                                                          evidence_id, properties_json, created_at, updated_at)
                        SELECT DISTINCT source_nodes.id, target_nodes.id, edges.edge_type, edges.weight,
                               edges.evidence_id, edges.properties_json, edges.created_at, edges.updated_at
                        FROM knowledge_graph_edges_legacy edges
                        JOIN knowledge_graph_nodes_legacy legacy_source_nodes ON legacy_source_nodes.id = edges.source_node_row_id
                        JOIN knowledge_graph_nodes_legacy legacy_target_nodes ON legacy_target_nodes.id = edges.target_node_row_id
                        JOIN knowledge_graph_nodes source_nodes ON source_nodes.node_id = legacy_source_nodes.node_id
                        JOIN knowledge_graph_nodes target_nodes ON target_nodes.node_id = legacy_target_nodes.node_id
                        """);
            } else {
                statement.executeUpdate("""
                        INSERT INTO knowledge_graph_edges(source_node_row_id, target_node_row_id, edge_type, weight,
                                                          evidence_id, properties_json, created_at, updated_at)
                        SELECT DISTINCT source_nodes.id, target_nodes.id, edges.edge_type, edges.weight,
                               edges.evidence_id, edges.properties_json, edges.created_at, edges.updated_at
                        FROM knowledge_graph_edges_legacy edges
                        JOIN knowledge_graph_nodes source_nodes ON source_nodes.node_id = edges.source_node_id
                        JOIN knowledge_graph_nodes target_nodes ON target_nodes.node_id = edges.target_node_id
                        """);
            }
        }
        dropLegacyTables(statement, "knowledge_graph_edges_legacy", "knowledge_graph_nodes_legacy");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void dropAgentIndexes(Statement statement) throws SQLException {
        statement.executeUpdate("DROP INDEX IF EXISTS idx_agent_runs_finished_at");
        statement.executeUpdate("DROP INDEX IF EXISTS idx_agent_tool_calls_status_finished_at");
    }

    private void dropKnowledgeGraphIndexes(Statement statement) throws SQLException {
        statement.executeUpdate("DROP INDEX IF EXISTS idx_kg_nodes_type_name");
        statement.executeUpdate("DROP INDEX IF EXISTS idx_kg_edges_source_type");
        statement.executeUpdate("DROP INDEX IF EXISTS idx_kg_edges_target_type");
        statement.executeUpdate("DROP INDEX IF EXISTS idx_kg_edges_evidence");
        statement.executeUpdate("DROP INDEX IF EXISTS uk_kg_edges_relation_evidence");
    }

    private void dropLegacyTables(Statement statement, String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + tableName);
        }
    }
}

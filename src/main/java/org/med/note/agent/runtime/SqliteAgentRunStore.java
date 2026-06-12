package org.med.note.agent.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.med.note.dto.AgentStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-backed agent audit store. The schema mirrors simple MySQL-style tables while keeping
 * JSON payloads for flexible troubleshooting snapshots.
 */
@Component
public class SqliteAgentRunStore implements AgentRunStore {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final int maxRecords;
    private final String sqlitePath;

    public SqliteAgentRunStore(
            DataSource dataSource,
            ObjectMapper objectMapper,
            @Value("${mednote.agent.session.max-records:100}") int maxRecords,
            @Value("${mednote.agent.store.sqlite.path:data/mednote-agent.db}") String sqlitePath
    ) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.maxRecords = Math.max(1, maxRecords);
        this.sqlitePath = sqlitePath;
    }

    @PostConstruct
    public void initialize() {
        ensureDatabaseDirectory();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
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
        } catch (SQLException error) {
            throw new IllegalStateException("初始化 agent SQLite 存储失败", error);
        }
    }

    @Override
    public synchronized AgentRunRecord save(AgentSession session, List<AgentStep> steps, Instant finishedAt) {
        AgentRunRecord record = new AgentRunRecord(
                session.id(),
                session.startedAt(),
                finishedAt,
                session.toolCalls(),
                List.copyOf(steps)
        );
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            deleteSession(connection, session.id());
            insertRun(connection, record);
            insertSteps(connection, record.sessionId(), steps);
            insertToolCalls(connection, record.toolCalls());
            evictOldestIfNeeded(connection);
            connection.commit();
            return record;
        } catch (SQLException error) {
            throw new IllegalStateException("保存 agent 会话失败: " + session.id(), error);
        }
    }

    @Override
    public synchronized Optional<AgentRunRecord> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT payload_json FROM agent_runs WHERE session_id = ?")) {
            statement.setString(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(read(resultSet.getString("payload_json"), AgentRunRecord.class));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("查询 agent 会话失败: " + sessionId, error);
        }
    }

    @Override
    public synchronized List<AgentRunRecord> recent(int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT payload_json
                     FROM agent_runs
                     ORDER BY finished_at DESC
                     LIMIT ?
                     """)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<AgentRunRecord> records = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    records.add(read(resultSet.getString("payload_json"), AgentRunRecord.class));
                }
                return records;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("查询最近 agent 会话失败", error);
        }
    }

    @Override
    public synchronized List<ToolCallRecord> failedToolCalls(int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT payload_json
                     FROM agent_tool_calls
                     WHERE status = ?
                     ORDER BY finished_at DESC
                     LIMIT ?
                     """)) {
            statement.setString(1, ToolExecutionStatus.FAILED.name());
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<ToolCallRecord> records = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    records.add(read(resultSet.getString("payload_json"), ToolCallRecord.class));
                }
                return records;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("查询失败工具调用失败", error);
        }
    }

    private void ensureDatabaseDirectory() {
        Path path = Path.of(sqlitePath).toAbsolutePath();
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException error) {
            throw new IllegalStateException("创建 SQLite 存储目录失败: " + parent, error);
        }
    }

    private void insertRun(Connection connection, AgentRunRecord record) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO agent_runs(session_id, started_at, finished_at, tool_call_count, step_count, payload_json)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, record.sessionId());
            statement.setString(2, record.startedAt().toString());
            statement.setString(3, record.finishedAt().toString());
            statement.setInt(4, record.toolCalls().size());
            statement.setInt(5, record.steps().size());
            statement.setString(6, write(record));
            statement.executeUpdate();
        }
    }

    private void insertSteps(Connection connection, String sessionId, List<AgentStep> steps) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO agent_steps(session_id, step_order, stage, event_type, status, content, metadata_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (AgentStep step : steps) {
                statement.setString(1, sessionId);
                statement.setInt(2, step.order());
                statement.setString(3, step.stage());
                statement.setString(4, step.eventType());
                statement.setString(5, step.status());
                statement.setString(6, step.content());
                statement.setString(7, write(step.metadata()));
                statement.setString(8, step.createdAt().toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertToolCalls(Connection connection, List<ToolCallRecord> toolCalls) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO agent_tool_calls(
                    session_id, tool_order, tool_name, phase, status, started_at, finished_at, duration_ms,
                    summary, input_snapshot_json, output_metadata_json, failure_type, error_type, error_message, payload_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (ToolCallRecord toolCall : toolCalls) {
                statement.setString(1, toolCall.sessionId());
                statement.setInt(2, toolCall.order());
                statement.setString(3, toolCall.toolName());
                statement.setString(4, toolCall.phase());
                statement.setString(5, toolCall.status().name());
                statement.setString(6, toolCall.startedAt().toString());
                statement.setString(7, toolCall.finishedAt().toString());
                statement.setLong(8, toolCall.durationMs());
                statement.setString(9, toolCall.summary());
                statement.setString(10, write(toolCall.inputSnapshot()));
                statement.setString(11, write(toolCall.outputMetadata()));
                statement.setString(12, toolCall.failureType().name());
                statement.setString(13, toolCall.errorType());
                statement.setString(14, toolCall.errorMessage());
                statement.setString(15, write(toolCall));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void deleteSession(Connection connection, String sessionId) throws SQLException {
        try (PreparedStatement deleteSteps = connection.prepareStatement("DELETE FROM agent_steps WHERE session_id = ?");
             PreparedStatement deleteToolCalls = connection.prepareStatement("DELETE FROM agent_tool_calls WHERE session_id = ?");
             PreparedStatement deleteRun = connection.prepareStatement("DELETE FROM agent_runs WHERE session_id = ?")) {
            deleteSteps.setString(1, sessionId);
            deleteSteps.executeUpdate();
            deleteToolCalls.setString(1, sessionId);
            deleteToolCalls.executeUpdate();
            deleteRun.setString(1, sessionId);
            deleteRun.executeUpdate();
        }
    }

    private void evictOldestIfNeeded(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT session_id
                FROM agent_runs
                ORDER BY finished_at DESC
                LIMIT -1 OFFSET ?
                """)) {
            statement.setInt(1, maxRecords);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    deleteSession(connection, resultSet.getString("session_id"));
                }
            }
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("序列化 agent 存储对象失败", error);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("反序列化 agent 存储对象失败: " + type.getSimpleName(), error);
        }
    }
}

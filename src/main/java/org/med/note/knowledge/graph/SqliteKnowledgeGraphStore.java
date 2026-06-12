package org.med.note.knowledge.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQLite implementation of the knowledge graph store.
 *
 * <p>The schema deliberately uses business ids and JSON properties so the storage can simulate
 * a MySQL table model today and later move to MySQL or a graph database without changing callers.</p>
 */
@Component
public class SqliteKnowledgeGraphStore implements KnowledgeGraphStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public SqliteKnowledgeGraphStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
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
        } catch (SQLException error) {
            throw new IllegalStateException("初始化知识图谱 SQLite 存储失败", error);
        }
    }

    @Override
    public synchronized KnowledgeGraphNode upsertNode(KnowledgeGraphNode node) {
        try (Connection connection = dataSource.getConnection()) {
            upsertNode(connection, node);
            return node;
        } catch (SQLException error) {
            throw new IllegalStateException("写入知识图谱节点失败: " + node.id(), error);
        }
    }

    @Override
    public synchronized KnowledgeGraphEdge upsertEdge(KnowledgeGraphEdge edge) {
        try (Connection connection = dataSource.getConnection()) {
            upsertEdge(connection, edge);
            return edge;
        } catch (SQLException error) {
            throw new IllegalStateException("写入知识图谱边失败: " + edge.id(), error);
        }
    }

    @Override
    public synchronized void upsertSubgraph(List<KnowledgeGraphNode> nodes, List<KnowledgeGraphEdge> edges) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (KnowledgeGraphNode node : nodes == null ? List.<KnowledgeGraphNode>of() : nodes) {
                upsertNode(connection, node);
            }
            for (KnowledgeGraphEdge edge : edges == null ? List.<KnowledgeGraphEdge>of() : edges) {
                upsertEdge(connection, edge);
            }
            connection.commit();
        } catch (SQLException error) {
            throw new IllegalStateException("批量写入知识图谱失败", error);
        }
    }

    @Override
    public Optional<KnowledgeGraphNode> findNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM knowledge_graph_nodes WHERE node_id = ?")) {
            statement.setString(1, nodeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readNode(resultSet)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("读取知识图谱节点失败: " + nodeId, error);
        }
    }

    @Override
    public List<KnowledgeGraphNode> searchNodes(String keyword, int limit) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT *
                     FROM knowledge_graph_nodes
                     WHERE name LIKE ? OR canonical_name LIKE ? OR source_id LIKE ?
                     ORDER BY node_type, canonical_name
                     LIMIT ?
                     """)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setInt(4, Math.max(1, limit));
            return readNodes(statement);
        } catch (SQLException error) {
            throw new IllegalStateException("搜索知识图谱节点失败: " + keyword, error);
        }
    }

    @Override
    public List<KnowledgeGraphEdge> findOutgoingEdges(String sourceNodeId, String edgeType, int limit) {
        return findEdges("source_node_id", sourceNodeId, edgeType, limit);
    }

    @Override
    public List<KnowledgeGraphEdge> findIncomingEdges(String targetNodeId, String edgeType, int limit) {
        return findEdges("target_node_id", targetNodeId, edgeType, limit);
    }

    @Override
    public long countNodes() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(1) FROM knowledge_graph_nodes");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        } catch (SQLException error) {
            throw new IllegalStateException("统计知识图谱节点失败", error);
        }
    }

    private List<KnowledgeGraphEdge> findEdges(String column, String nodeId, String edgeType, int limit) {
        if (nodeId == null || nodeId.isBlank()) {
            return List.of();
        }
        String sql = """
                SELECT *
                FROM knowledge_graph_edges
                WHERE %s = ? AND (? = '' OR edge_type = ?)
                ORDER BY weight DESC, updated_at DESC
                LIMIT ?
                """.formatted(column);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String safeEdgeType = edgeType == null ? "" : edgeType;
            statement.setString(1, nodeId);
            statement.setString(2, safeEdgeType);
            statement.setString(3, safeEdgeType);
            statement.setInt(4, Math.max(1, limit));
            return readEdges(statement);
        } catch (SQLException error) {
            throw new IllegalStateException("读取知识图谱边失败: " + nodeId, error);
        }
    }

    private void upsertNode(Connection connection, KnowledgeGraphNode node) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO knowledge_graph_nodes(
                    node_id, node_type, name, canonical_name, source_id, properties_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(node_id) DO UPDATE SET
                    node_type = excluded.node_type,
                    name = excluded.name,
                    canonical_name = excluded.canonical_name,
                    source_id = excluded.source_id,
                    properties_json = excluded.properties_json,
                    updated_at = excluded.updated_at
                """)) {
            statement.setString(1, node.id());
            statement.setString(2, node.type());
            statement.setString(3, node.name());
            statement.setString(4, node.canonicalName());
            statement.setString(5, node.sourceId());
            statement.setString(6, write(node.properties()));
            statement.setString(7, node.createdAt().toString());
            statement.setString(8, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private void upsertEdge(Connection connection, KnowledgeGraphEdge edge) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO knowledge_graph_edges(
                    edge_id, source_node_id, target_node_id, edge_type, weight, evidence_id,
                    properties_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(edge_id) DO UPDATE SET
                    source_node_id = excluded.source_node_id,
                    target_node_id = excluded.target_node_id,
                    edge_type = excluded.edge_type,
                    weight = excluded.weight,
                    evidence_id = excluded.evidence_id,
                    properties_json = excluded.properties_json,
                    updated_at = excluded.updated_at
                """)) {
            statement.setString(1, edge.id());
            statement.setString(2, edge.sourceNodeId());
            statement.setString(3, edge.targetNodeId());
            statement.setString(4, edge.type());
            statement.setDouble(5, edge.weight());
            statement.setString(6, edge.evidenceId());
            statement.setString(7, write(edge.properties()));
            statement.setString(8, edge.createdAt().toString());
            statement.setString(9, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private List<KnowledgeGraphNode> readNodes(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<KnowledgeGraphNode> nodes = new ArrayList<>();
            while (resultSet.next()) {
                nodes.add(readNode(resultSet));
            }
            return nodes;
        }
    }

    private List<KnowledgeGraphEdge> readEdges(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<KnowledgeGraphEdge> edges = new ArrayList<>();
            while (resultSet.next()) {
                edges.add(readEdge(resultSet));
            }
            return edges;
        }
    }

    private KnowledgeGraphNode readNode(ResultSet resultSet) throws SQLException {
        return new KnowledgeGraphNode(
                resultSet.getString("node_id"),
                resultSet.getString("node_type"),
                resultSet.getString("name"),
                resultSet.getString("canonical_name"),
                resultSet.getString("source_id"),
                readMap(resultSet.getString("properties_json")),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    private KnowledgeGraphEdge readEdge(ResultSet resultSet) throws SQLException {
        return new KnowledgeGraphEdge(
                resultSet.getString("edge_id"),
                resultSet.getString("source_node_id"),
                resultSet.getString("target_node_id"),
                resultSet.getString("edge_type"),
                resultSet.getDouble("weight"),
                resultSet.getString("evidence_id"),
                readMap(resultSet.getString("properties_json")),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("序列化知识图谱属性失败", error);
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value == null || value.isBlank() ? "{}" : value, MAP_TYPE);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("反序列化知识图谱属性失败", error);
        }
    }
}

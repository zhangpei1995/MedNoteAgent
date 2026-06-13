package org.med.note.knowledge.graph;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.med.note.dao.KnowledgeGraphEdgeMapper;
import org.med.note.dao.KnowledgeGraphNodeMapper;
import org.med.note.persistence.entity.KnowledgeGraphEdgeEntity;
import org.med.note.persistence.entity.KnowledgeGraphNodeEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Knowledge graph store implemented through MyBatis-Plus mappers.
 */
@Component
public class SqliteKnowledgeGraphStore implements KnowledgeGraphStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final KnowledgeGraphNodeMapper nodeMapper;
    private final KnowledgeGraphEdgeMapper edgeMapper;
    private final ObjectMapper objectMapper;

    public SqliteKnowledgeGraphStore(
            KnowledgeGraphNodeMapper nodeMapper,
            KnowledgeGraphEdgeMapper edgeMapper,
            ObjectMapper objectMapper
    ) {
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public synchronized KnowledgeGraphNode upsertNode(KnowledgeGraphNode node) {
        KnowledgeGraphNodeEntity existing = nodeMapper.selectById(node.id());
        KnowledgeGraphNodeEntity entity = toNodeEntity(node, existing);
        if (existing == null) {
            nodeMapper.insert(entity);
        } else {
            nodeMapper.updateById(entity);
        }
        return node;
    }

    @Override
    @Transactional
    public synchronized KnowledgeGraphEdge upsertEdge(KnowledgeGraphEdge edge) {
        KnowledgeGraphEdgeEntity existing = edgeMapper.selectById(edge.id());
        KnowledgeGraphEdgeEntity entity = toEdgeEntity(edge, existing);
        if (existing == null) {
            edgeMapper.insert(entity);
        } else {
            edgeMapper.updateById(entity);
        }
        return edge;
    }

    @Override
    @Transactional
    public synchronized void upsertSubgraph(List<KnowledgeGraphNode> nodes, List<KnowledgeGraphEdge> edges) {
        for (KnowledgeGraphNode node : nodes == null ? List.<KnowledgeGraphNode>of() : nodes) {
            upsertNode(node);
        }
        for (KnowledgeGraphEdge edge : edges == null ? List.<KnowledgeGraphEdge>of() : edges) {
            upsertEdge(edge);
        }
    }

    @Override
    public Optional<KnowledgeGraphNode> findNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        KnowledgeGraphNodeEntity entity = nodeMapper.selectById(nodeId);
        return entity == null ? Optional.empty() : Optional.of(toNode(entity));
    }

    @Override
    public List<KnowledgeGraphNode> searchNodes(String keyword, int limit) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        return nodeMapper.selectList(new LambdaQueryWrapper<KnowledgeGraphNodeEntity>()
                        .like(KnowledgeGraphNodeEntity::getName, safeKeyword)
                        .or()
                        .like(KnowledgeGraphNodeEntity::getCanonicalName, safeKeyword)
                        .or()
                        .like(KnowledgeGraphNodeEntity::getSourceId, safeKeyword)
                        .orderByAsc(KnowledgeGraphNodeEntity::getNodeType, KnowledgeGraphNodeEntity::getCanonicalName)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(this::toNode)
                .toList();
    }

    @Override
    public List<KnowledgeGraphEdge> findOutgoingEdges(String sourceNodeId, String edgeType, int limit) {
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeGraphEdgeEntity> query = new LambdaQueryWrapper<KnowledgeGraphEdgeEntity>()
                .eq(KnowledgeGraphEdgeEntity::getSourceNodeId, sourceNodeId)
                .orderByDesc(KnowledgeGraphEdgeEntity::getWeight)
                .orderByDesc(KnowledgeGraphEdgeEntity::getUpdatedAt)
                .last("LIMIT " + Math.max(1, limit));
        if (edgeType != null && !edgeType.isBlank()) {
            query.eq(KnowledgeGraphEdgeEntity::getEdgeType, edgeType);
        }
        return edgeMapper.selectList(query).stream().map(this::toEdge).toList();
    }

    @Override
    public List<KnowledgeGraphEdge> findIncomingEdges(String targetNodeId, String edgeType, int limit) {
        if (targetNodeId == null || targetNodeId.isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeGraphEdgeEntity> query = new LambdaQueryWrapper<KnowledgeGraphEdgeEntity>()
                .eq(KnowledgeGraphEdgeEntity::getTargetNodeId, targetNodeId)
                .orderByDesc(KnowledgeGraphEdgeEntity::getWeight)
                .orderByDesc(KnowledgeGraphEdgeEntity::getUpdatedAt)
                .last("LIMIT " + Math.max(1, limit));
        if (edgeType != null && !edgeType.isBlank()) {
            query.eq(KnowledgeGraphEdgeEntity::getEdgeType, edgeType);
        }
        return edgeMapper.selectList(query).stream().map(this::toEdge).toList();
    }

    @Override
    public long countNodes() {
        return nodeMapper.selectCount(null);
    }

    private KnowledgeGraphNodeEntity toNodeEntity(KnowledgeGraphNode node, KnowledgeGraphNodeEntity existing) {
        KnowledgeGraphNodeEntity entity = new KnowledgeGraphNodeEntity();
        entity.setNodeId(node.id());
        entity.setNodeType(node.type());
        entity.setName(node.name());
        entity.setCanonicalName(node.canonicalName());
        entity.setSourceId(node.sourceId());
        entity.setPropertiesJson(write(node.properties()));
        entity.setCreatedAt(existing == null ? node.createdAt().toString() : existing.getCreatedAt());
        entity.setUpdatedAt(Instant.now().toString());
        return entity;
    }

    private KnowledgeGraphEdgeEntity toEdgeEntity(KnowledgeGraphEdge edge, KnowledgeGraphEdgeEntity existing) {
        KnowledgeGraphEdgeEntity entity = new KnowledgeGraphEdgeEntity();
        entity.setEdgeId(edge.id());
        entity.setSourceNodeId(edge.sourceNodeId());
        entity.setTargetNodeId(edge.targetNodeId());
        entity.setEdgeType(edge.type());
        entity.setWeight(edge.weight());
        entity.setEvidenceId(edge.evidenceId());
        entity.setPropertiesJson(write(edge.properties()));
        entity.setCreatedAt(existing == null ? edge.createdAt().toString() : existing.getCreatedAt());
        entity.setUpdatedAt(Instant.now().toString());
        return entity;
    }

    private KnowledgeGraphNode toNode(KnowledgeGraphNodeEntity entity) {
        return new KnowledgeGraphNode(
                entity.getNodeId(),
                entity.getNodeType(),
                entity.getName(),
                entity.getCanonicalName(),
                entity.getSourceId(),
                readMap(entity.getPropertiesJson()),
                Instant.parse(entity.getCreatedAt()),
                Instant.parse(entity.getUpdatedAt())
        );
    }

    private KnowledgeGraphEdge toEdge(KnowledgeGraphEdgeEntity entity) {
        return new KnowledgeGraphEdge(
                entity.getEdgeId(),
                entity.getSourceNodeId(),
                entity.getTargetNodeId(),
                entity.getEdgeType(),
                entity.getWeight(),
                entity.getEvidenceId(),
                readMap(entity.getPropertiesJson()),
                Instant.parse(entity.getCreatedAt()),
                Instant.parse(entity.getUpdatedAt())
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

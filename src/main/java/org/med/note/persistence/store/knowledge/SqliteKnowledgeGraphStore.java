package org.med.note.persistence.store.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.med.note.knowledge.graph.KnowledgeGraphEdge;
import org.med.note.knowledge.graph.KnowledgeGraphNode;
import org.med.note.knowledge.graph.KnowledgeGraphStore;
import org.med.note.persistence.mapper.KnowledgeGraphEdgeMapper;
import org.med.note.persistence.mapper.KnowledgeGraphNodeMapper;
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
        KnowledgeGraphNodeEntity existing = findNodeEntity(node.id()).orElse(null);
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
        KnowledgeGraphNodeEntity sourceNode = findNodeEntity(edge.sourceNodeId())
                .orElseThrow(() -> new IllegalArgumentException("知识图谱边缺少来源节点: " + edge.sourceNodeId()));
        KnowledgeGraphNodeEntity targetNode = findNodeEntity(edge.targetNodeId())
                .orElseThrow(() -> new IllegalArgumentException("知识图谱边缺少目标节点: " + edge.targetNodeId()));
        KnowledgeGraphEdgeEntity existing = findEdgeEntity(sourceNode.getId(), targetNode.getId(), edge.type(), edge.evidenceId())
                .orElse(null);
        KnowledgeGraphEdgeEntity entity = toEdgeEntity(edge, sourceNode, targetNode, existing);
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
        return findNodeEntity(nodeId).map(this::toNode);
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
        Optional<KnowledgeGraphNodeEntity> sourceNode = findNodeEntity(sourceNodeId);
        if (sourceNode.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeGraphEdgeEntity> query = new LambdaQueryWrapper<KnowledgeGraphEdgeEntity>()
                .eq(KnowledgeGraphEdgeEntity::getSourceNodeRowId, sourceNode.get().getId())
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
        Optional<KnowledgeGraphNodeEntity> targetNode = findNodeEntity(targetNodeId);
        if (targetNode.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeGraphEdgeEntity> query = new LambdaQueryWrapper<KnowledgeGraphEdgeEntity>()
                .eq(KnowledgeGraphEdgeEntity::getTargetNodeRowId, targetNode.get().getId())
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
        entity.setId(existing == null ? null : existing.getId());
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

    private KnowledgeGraphEdgeEntity toEdgeEntity(
            KnowledgeGraphEdge edge,
            KnowledgeGraphNodeEntity sourceNode,
            KnowledgeGraphNodeEntity targetNode,
            KnowledgeGraphEdgeEntity existing
    ) {
        KnowledgeGraphEdgeEntity entity = new KnowledgeGraphEdgeEntity();
        entity.setId(existing == null ? null : existing.getId());
        entity.setSourceNodeRowId(sourceNode.getId());
        entity.setTargetNodeRowId(targetNode.getId());
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
                resolveNodeId(entity.getSourceNodeRowId()),
                resolveNodeId(entity.getTargetNodeRowId()),
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

    private Optional<KnowledgeGraphNodeEntity> findNodeEntity(String nodeId) {
        return Optional.ofNullable(nodeMapper.selectOne(new LambdaQueryWrapper<KnowledgeGraphNodeEntity>()
                .eq(KnowledgeGraphNodeEntity::getNodeId, nodeId)
                .last("LIMIT 1")));
    }

    private Optional<KnowledgeGraphEdgeEntity> findEdgeEntity(Long sourceNodeRowId, Long targetNodeRowId, String edgeType, String evidenceId) {
        return Optional.ofNullable(edgeMapper.selectOne(new LambdaQueryWrapper<KnowledgeGraphEdgeEntity>()
                .eq(KnowledgeGraphEdgeEntity::getSourceNodeRowId, sourceNodeRowId)
                .eq(KnowledgeGraphEdgeEntity::getTargetNodeRowId, targetNodeRowId)
                .eq(KnowledgeGraphEdgeEntity::getEdgeType, edgeType)
                .eq(KnowledgeGraphEdgeEntity::getEvidenceId, evidenceId == null ? "" : evidenceId)
                .last("LIMIT 1")));
    }

    private String resolveNodeId(Long nodeRowId) {
        KnowledgeGraphNodeEntity node = nodeMapper.selectById(nodeRowId);
        if (node == null) {
            throw new IllegalStateException("知识图谱边关联了不存在的节点自增 ID: " + nodeRowId);
        }
        return node.getNodeId();
    }

    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value == null || value.isBlank() ? "{}" : value, MAP_TYPE);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("反序列化知识图谱属性失败", error);
        }
    }
}

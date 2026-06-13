package org.med.note.persistence.store.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.med.note.agent.api.AgentStep;
import org.med.note.agent.runtime.AgentRunRecord;
import org.med.note.agent.runtime.AgentRunStore;
import org.med.note.agent.runtime.AgentSession;
import org.med.note.agent.runtime.ToolCallRecord;
import org.med.note.agent.runtime.ToolExecutionStatus;
import org.med.note.persistence.mapper.AgentRunMapper;
import org.med.note.persistence.mapper.AgentStepMapper;
import org.med.note.persistence.mapper.AgentToolCallMapper;
import org.med.note.persistence.entity.AgentRunEntity;
import org.med.note.persistence.entity.AgentStepEntity;
import org.med.note.persistence.entity.AgentToolCallEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis-Plus backed agent audit store.
 */
@Component
public class SqliteAgentRunStore implements AgentRunStore {

    private final AgentRunMapper runMapper;
    private final AgentStepMapper stepMapper;
    private final AgentToolCallMapper toolCallMapper;
    private final ObjectMapper objectMapper;
    private final int maxRecords;

    public SqliteAgentRunStore(
            AgentRunMapper runMapper,
            AgentStepMapper stepMapper,
            AgentToolCallMapper toolCallMapper,
            ObjectMapper objectMapper,
            @Value("${mednote.agent.session.max-records:100}") int maxRecords
    ) {
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
        this.toolCallMapper = toolCallMapper;
        this.objectMapper = objectMapper;
        this.maxRecords = Math.max(1, maxRecords);
    }

    @Override
    @Transactional
    public synchronized AgentRunRecord save(AgentSession session, List<AgentStep> steps, Instant finishedAt) {
        AgentRunRecord record = new AgentRunRecord(
                session.id(),
                session.startedAt(),
                finishedAt,
                session.toolCalls(),
                List.copyOf(steps)
        );
        deleteSession(session.id());
        AgentRunEntity runEntity = toRunEntity(record);
        runMapper.insert(runEntity);
        Long agentRunId = ensureRunId(runEntity.getId(), record.sessionId());
        for (AgentStep step : steps) {
            stepMapper.insert(toStepEntity(agentRunId, step));
        }
        for (ToolCallRecord toolCall : record.toolCalls()) {
            toolCallMapper.insert(toToolCallEntity(agentRunId, toolCall));
        }
        evictOldestIfNeeded();
        return record;
    }

    @Override
    public synchronized Optional<AgentRunRecord> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        AgentRunEntity entity = findRunEntity(sessionId).orElse(null);
        return entity == null ? Optional.empty() : Optional.of(read(entity.getPayloadJson(), AgentRunRecord.class));
    }

    @Override
    public synchronized List<AgentRunRecord> recent(int limit) {
        return runMapper.selectList(new LambdaQueryWrapper<AgentRunEntity>()
                        .orderByDesc(AgentRunEntity::getFinishedAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(entity -> read(entity.getPayloadJson(), AgentRunRecord.class))
                .toList();
    }

    @Override
    public synchronized List<ToolCallRecord> failedToolCalls(int limit) {
        return toolCallMapper.selectList(new LambdaQueryWrapper<AgentToolCallEntity>()
                        .eq(AgentToolCallEntity::getStatus, ToolExecutionStatus.FAILED.name())
                        .orderByDesc(AgentToolCallEntity::getFinishedAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(entity -> read(entity.getPayloadJson(), ToolCallRecord.class))
                .toList();
    }

    private AgentRunEntity toRunEntity(AgentRunRecord record) {
        AgentRunEntity entity = new AgentRunEntity();
        entity.setSessionId(record.sessionId());
        entity.setStartedAt(record.startedAt().toString());
        entity.setFinishedAt(record.finishedAt().toString());
        entity.setToolCallCount(record.toolCalls().size());
        entity.setStepCount(record.steps().size());
        entity.setPayloadJson(write(record));
        entity.setCreatedAt(Instant.now().toString());
        return entity;
    }

    private AgentStepEntity toStepEntity(Long agentRunId, AgentStep step) {
        AgentStepEntity entity = new AgentStepEntity();
        entity.setAgentRunId(agentRunId);
        entity.setStepOrder(step.order());
        entity.setStage(step.stage());
        entity.setEventType(step.eventType());
        entity.setStatus(step.status());
        entity.setContent(step.content());
        entity.setMetadataJson(write(step.metadata()));
        entity.setCreatedAt(step.createdAt().toString());
        return entity;
    }

    private AgentToolCallEntity toToolCallEntity(Long agentRunId, ToolCallRecord toolCall) {
        AgentToolCallEntity entity = new AgentToolCallEntity();
        entity.setAgentRunId(agentRunId);
        entity.setToolOrder(toolCall.order());
        entity.setToolName(toolCall.toolName());
        entity.setPhase(toolCall.phase());
        entity.setStatus(toolCall.status().name());
        entity.setStartedAt(toolCall.startedAt().toString());
        entity.setFinishedAt(toolCall.finishedAt().toString());
        entity.setDurationMs(toolCall.durationMs());
        entity.setSummary(toolCall.summary());
        entity.setInputSnapshotJson(write(toolCall.inputSnapshot()));
        entity.setOutputMetadataJson(write(toolCall.outputMetadata()));
        entity.setFailureType(toolCall.failureType().name());
        entity.setErrorType(toolCall.errorType());
        entity.setErrorMessage(toolCall.errorMessage());
        entity.setPayloadJson(write(toolCall));
        return entity;
    }

    private void deleteSession(String sessionId) {
        findRunEntity(sessionId).ifPresent(run -> {
            stepMapper.delete(new LambdaQueryWrapper<AgentStepEntity>().eq(AgentStepEntity::getAgentRunId, run.getId()));
            toolCallMapper.delete(new LambdaQueryWrapper<AgentToolCallEntity>().eq(AgentToolCallEntity::getAgentRunId, run.getId()));
            runMapper.deleteById(run.getId());
        });
    }

    private void evictOldestIfNeeded() {
        List<AgentRunEntity> expiredRuns = runMapper.selectList(new LambdaQueryWrapper<AgentRunEntity>()
                .select(AgentRunEntity::getSessionId)
                .orderByDesc(AgentRunEntity::getFinishedAt)
                .last("LIMIT -1 OFFSET " + maxRecords));
        for (AgentRunEntity expiredRun : expiredRuns) {
            deleteSession(expiredRun.getSessionId());
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("序列化 agent 存储对象失败", error);
        }
    }

    private Optional<AgentRunEntity> findRunEntity(String sessionId) {
        return Optional.ofNullable(runMapper.selectOne(new LambdaQueryWrapper<AgentRunEntity>()
                .eq(AgentRunEntity::getSessionId, sessionId)
                .last("LIMIT 1")));
    }

    private Long ensureRunId(Long id, String sessionId) {
        if (id != null) {
            return id;
        }
        return findRunEntity(sessionId)
                .map(AgentRunEntity::getId)
                .orElseThrow(() -> new IllegalStateException("保存 agent 会话后未获取到自增 ID: " + sessionId));
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("反序列化 agent 存储对象失败: " + type.getSimpleName(), error);
        }
    }
}

package org.med.note.agent.metadata;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.med.note.dao.ChatSessionMetadataMapper;
import org.med.note.domain.entity.ChatSessionMetadata;
import org.med.note.domain.metadata.SessionMetadataResult;
import org.med.note.domain.metadata.SessionMetadataStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 管理会话元数据异步生成状态和持久化结果。
 */
@Component
public class SessionMetadataLifecycleManager {

    private final ChatSessionMetadataMapper metadataMapper;

    public SessionMetadataLifecycleManager(ChatSessionMetadataMapper metadataMapper) {
        this.metadataMapper = metadataMapper;
    }

    /**
     * 创建或重置会话元数据为生成中。
     */
    @Transactional
    public ChatSessionMetadata markGenerating(String sessionId, String sourceTurnId) {
        LocalDateTime now = LocalDateTime.now();
        ChatSessionMetadata metadata = findBySessionId(sessionId);
        if (metadata == null) {
            metadata = new ChatSessionMetadata();
            metadata.setId(IdUtil.fastSimpleUUID());
            metadata.setSessionId(sessionId);
            metadata.setCreatedAt(now);
        }

        metadata.setSourceTurnId(sourceTurnId);
        metadata.setStatus(SessionMetadataStatus.GENERATING.name());
        metadata.setTitle(null);
        metadata.setConsultationCategory(null);
        metadata.setRecognizedDrugName(null);
        metadata.setInstructionItem(null);
        metadata.setKnowledgeStatus(null);
        metadata.setScopeStatus(null);
        metadata.setUnderstandingText(null);
        metadata.setMetadataJson(null);
        metadata.setErrorMessage(null);
        metadata.setGeneratedAt(null);
        metadata.setUpdatedAt(now);
        save(metadata);
        return metadata;
    }

    /**
     * 写入生成完成的会话元数据。
     */
    @Transactional
    public void markGenerated(String sessionId, String sourceTurnId, SessionMetadataResult result) {
        LocalDateTime now = LocalDateTime.now();
        ChatSessionMetadata metadata = findOrCreate(sessionId, now);
        metadata.setSourceTurnId(sourceTurnId);
        metadata.setStatus(SessionMetadataStatus.GENERATED.name());
        metadata.setTitle(StrUtil.blankToDefault(result.getTitle(), null));
        metadata.setConsultationCategory(result.getConsultationCategory() == null ? null : result.getConsultationCategory().name());
        metadata.setRecognizedDrugName(StrUtil.blankToDefault(result.getRecognizedDrugName(), null));
        metadata.setInstructionItem(StrUtil.blankToDefault(result.getInstructionItem(), null));
        metadata.setKnowledgeStatus(result.getKnowledgeStatus() == null ? null : result.getKnowledgeStatus().name());
        metadata.setScopeStatus(result.getScopeStatus() == null ? null : result.getScopeStatus().name());
        metadata.setUnderstandingText(StrUtil.blankToDefault(result.getUnderstandingText(), null));
        metadata.setMetadataJson(JSONUtil.toJsonStr(result));
        metadata.setErrorMessage(null);
        metadata.setGeneratedAt(now);
        metadata.setUpdatedAt(now);
        save(metadata);
    }

    /**
     * 记录元数据生成失败状态。
     */
    @Transactional
    public void markFailed(String sessionId, String sourceTurnId, Exception exception) {
        LocalDateTime now = LocalDateTime.now();
        ChatSessionMetadata metadata = findOrCreate(sessionId, now);
        metadata.setSourceTurnId(sourceTurnId);
        metadata.setStatus(SessionMetadataStatus.FAILED.name());
        metadata.setErrorMessage(ExceptionUtil.getRootCauseMessage(exception));
        metadata.setGeneratedAt(null);
        metadata.setUpdatedAt(now);
        save(metadata);
    }

    public ChatSessionMetadata findBySessionId(String sessionId) {
        return metadataMapper.selectOne(new LambdaQueryWrapper<ChatSessionMetadata>()
                .eq(ChatSessionMetadata::getSessionId, sessionId)
                .last("LIMIT 1"));
    }

    private ChatSessionMetadata findOrCreate(String sessionId, LocalDateTime now) {
        ChatSessionMetadata metadata = findBySessionId(sessionId);
        if (metadata != null) {
            return metadata;
        }

        metadata = new ChatSessionMetadata();
        metadata.setId(IdUtil.fastSimpleUUID());
        metadata.setSessionId(sessionId);
        metadata.setCreatedAt(now);
        return metadata;
    }

    private void save(ChatSessionMetadata metadata) {
        if (metadataMapper.selectById(metadata.getId()) == null) {
            metadataMapper.insert(metadata);
            return;
        }
        metadataMapper.updateById(metadata);
    }
}

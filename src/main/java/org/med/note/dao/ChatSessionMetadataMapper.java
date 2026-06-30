package org.med.note.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.med.note.domain.entity.ChatSessionMetadata;

/**
 * 会话元数据表访问接口。
 */
@Mapper
public interface ChatSessionMetadataMapper extends BaseMapper<ChatSessionMetadata> {
}

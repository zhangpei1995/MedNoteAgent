package org.med.note.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.med.note.persistence.entity.KnowledgeGraphEdgeEntity;

@Mapper
public interface KnowledgeGraphEdgeMapper extends BaseMapper<KnowledgeGraphEdgeEntity> {
}

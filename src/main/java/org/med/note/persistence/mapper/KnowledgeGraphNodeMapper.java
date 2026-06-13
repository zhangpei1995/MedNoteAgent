package org.med.note.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.med.note.persistence.entity.KnowledgeGraphNodeEntity;

@Mapper
public interface KnowledgeGraphNodeMapper extends BaseMapper<KnowledgeGraphNodeEntity> {
}

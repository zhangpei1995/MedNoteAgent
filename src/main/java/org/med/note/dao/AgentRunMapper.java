package org.med.note.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.med.note.persistence.entity.AgentRunEntity;

@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRunEntity> {
}

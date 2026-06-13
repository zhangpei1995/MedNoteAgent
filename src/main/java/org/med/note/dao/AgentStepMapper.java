package org.med.note.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.med.note.persistence.entity.AgentStepEntity;

@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStepEntity> {
}

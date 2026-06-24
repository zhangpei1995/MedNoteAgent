package org.med.note.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.med.note.domain.entity.ChatTurnAudit;

@Mapper
public interface ChatTurnAuditMapper extends BaseMapper<ChatTurnAudit> {
}

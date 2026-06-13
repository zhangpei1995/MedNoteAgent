package org.med.note.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("knowledge_graph_edges")
public class KnowledgeGraphEdgeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceNodeRowId;
    private Long targetNodeRowId;
    private String edgeType;
    private Double weight;
    private String evidenceId;
    private String propertiesJson;
    private String createdAt;
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceNodeRowId() {
        return sourceNodeRowId;
    }

    public void setSourceNodeRowId(Long sourceNodeRowId) {
        this.sourceNodeRowId = sourceNodeRowId;
    }

    public Long getTargetNodeRowId() {
        return targetNodeRowId;
    }

    public void setTargetNodeRowId(Long targetNodeRowId) {
        this.targetNodeRowId = targetNodeRowId;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getPropertiesJson() {
        return propertiesJson;
    }

    public void setPropertiesJson(String propertiesJson) {
        this.propertiesJson = propertiesJson;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

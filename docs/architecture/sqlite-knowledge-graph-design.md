# SQLite 知识图谱设计

## 1. 目标

知识图谱用于沉淀药品说明书中的结构化关系，支撑后续检索、证据追踪、图谱扩展和医学安全策略。

本阶段使用 SQLite 模拟 MySQL 表结构，目标不是一次性实现完整图数据库能力，而是先把节点、边、读写接口和本地持久化边界设计清楚。

## 2. 核心抽象

代码位置：

```text
src/main/java/org/med/note/knowledge/graph
├── KnowledgeGraphNode.java
├── KnowledgeGraphEdge.java
├── KnowledgeGraphReader.java
├── KnowledgeGraphWriter.java
├── KnowledgeGraphStore.java
├── SqliteKnowledgeGraphStore.java
└── KnowledgeGraphBootstrap.java
```

接口职责：

| 接口 | 职责 | 典型调用方 |
| --- | --- | --- |
| `KnowledgeGraphReader` | 查节点、搜索节点、查入边/出边、统计节点 | 检索、问答、调试接口 |
| `KnowledgeGraphWriter` | upsert 节点、upsert 边、批量写入子图 | 说明书入库、抽取流程 |
| `KnowledgeGraphStore` | 组合读写接口 | 本地 demo 或管理流程 |

读写分离的原因：

1. 问答链路通常只需要读，不应拿到写能力。
2. 入库链路需要批量写子图，应该集中处理事务。
3. 后续迁 MySQL 或图数据库时，上层只依赖接口。

## 3. 节点设计

`KnowledgeGraphNode`：

```text
id             稳定业务 ID，例如 drug:二冬汤颗粒
type           节点类型，例如 DRUG / INSTRUCTION_SECTION / EVIDENCE_CHUNK
name           展示名
canonicalName  归一化名称，用于搜索和去重
sourceId       来源 ID，例如证据片段 ID、说明书 ID
properties     扩展属性 JSON
createdAt      创建时间
updatedAt      更新时间
```

当前 demo 节点类型：

| 类型 | 含义 | 示例 |
| --- | --- | --- |
| `DRUG` | 药品 | 二冬汤颗粒 |
| `INSTRUCTION_SECTION` | 说明书章节 | 用法用量、禁忌 |
| `EVIDENCE_CHUNK` | 可追溯证据片段 | mock-cmxf-contraindication |

## 4. 边设计

`KnowledgeGraphEdge`：

```text
id             稳定业务 ID，例如 drug:x->HAS_SECTION->section:y
sourceNodeId   起点节点 ID
targetNodeId   终点节点 ID
type           关系类型
weight         权重，用于排序或置信度
evidenceId     支撑该关系的证据 ID
properties     扩展属性 JSON
createdAt      创建时间
updatedAt      更新时间
```

当前 demo 边类型：

| 类型 | 方向 | 含义 |
| --- | --- | --- |
| `HAS_SECTION` | `DRUG -> INSTRUCTION_SECTION` | 药品包含说明书章节 |
| `HAS_EVIDENCE` | `INSTRUCTION_SECTION -> EVIDENCE_CHUNK` | 章节下有证据片段 |
| `EVIDENCE_OF` | `EVIDENCE_CHUNK -> DRUG` | 证据片段归属药品 |

## 5. SQLite 表结构

### 5.1 knowledge_graph_nodes

```text
node_id          VARCHAR(128) PRIMARY KEY
node_type        VARCHAR(80) NOT NULL
name             VARCHAR(255) NOT NULL
canonical_name   VARCHAR(255) NOT NULL
source_id        VARCHAR(128) NOT NULL
properties_json  TEXT NOT NULL
created_at       VARCHAR(40) NOT NULL
updated_at       VARCHAR(40) NOT NULL
```

索引：

```text
idx_kg_nodes_type_name(node_type, canonical_name)
```

### 5.2 knowledge_graph_edges

```text
edge_id           VARCHAR(192) PRIMARY KEY
source_node_id    VARCHAR(128) NOT NULL
target_node_id    VARCHAR(128) NOT NULL
edge_type         VARCHAR(80) NOT NULL
weight            REAL NOT NULL
evidence_id       VARCHAR(128) NOT NULL
properties_json   TEXT NOT NULL
created_at        VARCHAR(40) NOT NULL
updated_at        VARCHAR(40) NOT NULL
```

索引：

```text
idx_kg_edges_source_type(source_node_id, edge_type)
idx_kg_edges_target_type(target_node_id, edge_type)
idx_kg_edges_evidence(evidence_id)
```

## 6. 写入逻辑

单节点写入：

```text
KnowledgeGraphWriter.upsertNode(node)
  -> INSERT node
  -> ON CONFLICT(node_id) UPDATE 可变字段和 updated_at
```

单边写入：

```text
KnowledgeGraphWriter.upsertEdge(edge)
  -> INSERT edge
  -> ON CONFLICT(edge_id) UPDATE 可变字段和 updated_at
```

批量子图写入：

```text
KnowledgeGraphWriter.upsertSubgraph(nodes, edges)
  -> 开启事务
  -> 先 upsert 所有节点
  -> 再 upsert 所有边
  -> commit
```

先写节点再写边，是为了保证边的外键引用存在。后续说明书入库链路应优先使用 `upsertSubgraph`，而不是多次单独写入。

## 7. 读取逻辑

节点读取：

```text
findNode(nodeId)
searchNodes(keyword, limit)
```

边读取：

```text
findOutgoingEdges(sourceNodeId, edgeType, limit)
findIncomingEdges(targetNodeId, edgeType, limit)
```

`edgeType` 允许为空，表示读取某节点的全部出边或入边。

## 8. Demo 初始化

`KnowledgeGraphBootstrap` 会在启动时检查 `countNodes()`：

```text
if graph is empty:
  read MockDrugKnowledgeBase.allEvidence()
  create DRUG nodes
  create INSTRUCTION_SECTION nodes
  create EVIDENCE_CHUNK nodes
  create HAS_SECTION / HAS_EVIDENCE / EVIDENCE_OF edges
  write by upsertSubgraph
```

这样本地 demo 首次启动即可拥有可读写的知识图谱。真实说明书入库完成后，这个 bootstrap 可以替换成 ingestion handler。

## 9. 后续迁移建议

迁 MySQL 时：

1. `properties_json` 改为 `JSON` 类型。
2. `created_at` / `updated_at` 改为 `DATETIME(3)`。
3. 增加唯一约束辅助去重，例如 `(node_type, canonical_name, source_id)`。
4. 外键增加 `ON DELETE CASCADE` 或显式软删除字段。
5. 如果图查询变复杂，保留 `KnowledgeGraphReader/Writer` 接口，新增 Neo4j 或 JanusGraph 实现。

## 10. 验证

已新增测试：

```text
src/test/java/org/med/note/knowledge/KnowledgeGraphStoreTest.java
```

已验证：

```text
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' test
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

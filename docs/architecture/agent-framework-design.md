# MedNoteAgent Agent 框架设计

本文是当前 Agent 架构的主设计文档，说明核心组件、模块边界、扩展点和存储基线。详细表结构见 [Agent 并发与 SQLite 存储设计](agent-concurrency-sqlite-design.md)，知识图谱详情见 [SQLite 知识图谱设计](sqlite-knowledge-graph-design.md)，PDF 入库详情见 [PDF 抽取与图谱设计](pdf-extraction-graph-design.md)。

## 1. 设计目标

MedNoteAgent 面向药品说明书知识管理和医学问答。回答必须基于已入库说明书证据，并保留可追溯来源。

当前阶段不是写 demo，而是在正式架构下先实现轻量能力：

- 说明书证据可以先来自本地处理数据。
- 检索、风险和回答可以先用规则或模板。
- 存储使用 SQLite。
- 存储层通过 MyBatis Plus 实现。
- 所有核心能力必须通过接口隔离，便于后续替换为真实 PDF 入库、混合检索、小模型规划和更强回答生成。

## 2. 架构原则

1. **证据优先**：没有说明书证据，不输出确定性医学结论。
2. **模块收敛**：Agent 相关编排、工具、运行时和专属服务优先收敛在 `agent` 边界。
3. **接口先行**：当前轻量实现也要依赖稳定接口，不把规则直接写死在编排层。
4. **存储明确**：当前使用 SQLite + MyBatis Plus，后续迁移 MySQL 或图数据库时保持上层接口稳定。
5. **工具克制**：工具代表可替换能力，不为每个小步骤新增工具。
6. **可观测**：每次 Agent 运行都应持久化工具链、输入快照、输出摘要和失败原因。

## 3. 当前总体结构

```text
controller
  -> agent
      -> MedNoteAgent
      -> runtime
          -> AgentToolPlanner
          -> AgentToolExecutor
          -> AgentContextMerger
          -> AgentRunStore
      -> tool
          -> request_planning
          -> drug_knowledge_search
          -> medical_risk_assessment
          -> answer_generation
  -> knowledge
      -> KnowledgeGraphReader
      -> KnowledgeGraphWriter
  -> persistence
      -> entity
      -> dao(MyBatis Plus Mapper)
  -> SQLite
```

核心请求链路：

```text
AgentRunRequest
  -> MedNoteAgent
  -> request_planning
  -> drug_knowledge_search
  -> medical_risk_assessment
  -> answer_generation
  -> AgentRunStore
  -> AgentRunResponse
```

## 4. 核心组件职责

| 组件 | 职责 | 不应承担 |
| --- | --- | --- |
| `controller` | HTTP/SSE 协议转换、参数校验、响应封装 | 医学规则、工具选择、Mapper 调用 |
| `MedNoteAgent` | 请求级编排、循环推进、运行结果保存 | 具体检索、风险规则、Prompt 细节 |
| `AgentToolPlanner` | 基于上下文、依赖和元数据选择工具批次 | 医学业务判断 |
| `AgentToolExecutor` | 执行工具、捕获异常、生成调用记录 | 合并上下文 |
| `AgentContextMerger` | 合并工具输出，形成下一步上下文 | 调用外部服务 |
| `AgentRunStore` | 保存和查询运行审计 | 业务规则 |
| `AgentTool` | 可替换能力入口 | 大量业务实现 |
| `service/spi` 或 Agent 专属接口 | 定义 planning、retrieval、risk、answer 能力 | 绑定具体数据源 |
| `knowledge` | 知识图谱读写接口和实现 | Agent 编排 |
| `dao` / `persistence/entity` | MyBatis Plus 持久化结构 | 业务流程 |

## 5. Agent 聚焦目录

Agent 和相关服务可以收敛到聚焦目录，但要保留清晰层次：

```text
agent/
├── MedNoteAgent.java
├── runtime/      # 调度、并发、审计、上下文合并
├── tool/         # 工具协议和工具入口
├── planning/     # 可选：Agent 专属规划接口和实现
├── retrieval/    # 可选：Agent 专属证据召回接口和实现
├── safety/       # 可选：Agent 专属风险评估接口和实现
└── answer/       # 可选：Agent 专属回答生成接口和实现
```

收敛判断：

- 只被 Agent 调用的能力，优先放在 `agent` 下。
- 会被入库、批处理、管理接口或其他上下文复用的能力，放到通用 `service`、`knowledge`、`client` 或 `persistence`。
- 不为了“分层完整”提前创建空目录；当某个能力真实变复杂时再拆子包。

## 6. 工具和能力接口

当前工具数量保持克制：

| 工具 | 依赖 | 职责 |
| --- | --- | --- |
| `request_planning` | 无 | 识别意图、关键词、查询目标和初步风险信号。 |
| `drug_knowledge_search` | `request_planning` | 根据计划召回说明书证据。 |
| `medical_risk_assessment` | `request_planning` | 根据问题和证据评估风险。 |
| `answer_generation` | `drug_knowledge_search`、`medical_risk_assessment` | 基于证据和风险生成最终回答。 |

工具只做入口和上下文转换，复杂逻辑放到接口实现中：

```text
RequestPlanner
EvidenceRetriever
RiskAssessor
AnswerGenerator
```

当前实现可以是规则、模板或本地数据，但类名应表达策略，不使用 demo 命名。后续接入小模型、混合检索或外部知识库时，应替换这些接口实现，而不是修改 `MedNoteAgent` 主流程。

## 7. 存储边界

当前存储链路：

```text
SQLite
  -> MyBatis Plus Mapper(dao)
  -> persistence/entity
  -> Store / Reader / Writer
  -> Agent / Knowledge / Service
```

已落地的主要存储：

- Agent 运行审计：`agent_runs`、`agent_steps`、`agent_tool_calls`。
- 知识图谱：`knowledge_graph_nodes`、`knowledge_graph_edges`。

规范：

- 业务层不直接调用 Mapper。
- Store / Reader / Writer 对上提供领域接口。
- SQLite 是当前正式存储方案，不写成临时或 demo 存储。
- 后续迁移 MySQL 时，应优先保持接口和实体语义稳定，再替换数据库方言和 schema 初始化方式。

## 8. 问答输出边界

回答结构应包含：

```text
answer
riskLevel
evidenceList
suggestedQuestions
needMoreInfo
missingInfo
traceId / sessionId
```

医学安全约束：

- 证据不足时说明无法判断。
- 合并用药、特殊人群、禁忌、不良反应默认提高风险等级。
- 不输出超出说明书证据的诊断、处方或剂量调整建议。
- 所有关键结论都应能回溯到证据片段。

## 9. 扩展顺序

后续扩展优先级：

1. 稳定 Agent 工具链和 SQLite 审计。
2. 将当前规则 planning 替换为小模型 JSON schema + 规则兜底。
3. 将本地证据召回替换为关键词 + 向量 + 图谱过滤的混合检索。
4. 完善 PDF 入库、证据切片和图谱写入。
5. 增加风险策略、回答校验和评测样本。

扩展时遵循一个原则：新增能力先看是否能作为现有接口的新实现；只有接口语义不足时，才新增接口或工具。

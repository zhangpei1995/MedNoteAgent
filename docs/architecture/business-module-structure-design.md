# 业务模块化目录结构设计

本文定义 MedNoteAgent 的代码组织标准。项目采用业务模块化单体结构：除数据存储层独立管理外，Controller、DTO、应用服务、领域接口和业务实现全部按业务上下文收敛，禁止横向 `controller`、`dto`、`service`、`impl` 目录继续膨胀。

## 1. 设计目标

1. 功能模块清晰，每个业务上下文内能看到入口、用例、领域模型和能力接口。
2. 代码聚焦，Agent、知识库、PDF 入库等能力不散落在多个技术目录中。
3. 存储层保持独立，MyBatis Plus Entity、Mapper、SQLite Store 实现和 schema 初始化集中管理。
4. 上层业务依赖领域接口，不直接依赖 Mapper、SQL 或具体数据库实现。
5. 保持单体工程，不拆 Maven 多模块，不引入复杂 DDD 目录。

## 2. 总体结构

目标包结构：

```text
src/main/java/org/med/note
├── MedNoteAgentApplication.java
├── agent/                         # 医学问答 Agent 业务上下文
│   ├── api/                       # Agent HTTP 入口、请求响应 DTO
│   ├── application/               # Agent 用例编排
│   ├── runtime/                   # 会话、工具调度、上下文合并、运行审计接口
│   ├── tool/                      # Agent 工具协议和工具入口
│   ├── planning/                  # 请求规划接口与实现
│   ├── retrieval/                 # Agent 证据召回接口
│   ├── safety/                    # 风险评估接口与实现
│   └── answer/                    # 回答生成接口与实现
├── knowledge/                     # 药品说明书知识业务上下文
│   ├── graph/                     # 知识图谱领域模型、读写接口
│   ├── evidence/                  # 证据片段、检索模型和知识证据仓储接口
│   └── ingestion/                 # PDF/说明书入库流程
├── persistence/                   # 独立数据存储层
│   ├── entity/                    # MyBatis Plus Entity
│   ├── mapper/                    # MyBatis Plus Mapper
│   ├── store/                     # Store / Repository / Reader / Writer 的数据库实现
│   │   ├── agent/
│   │   └── knowledge/
│   └── schema/                    # SQLite schema 初始化和迁移辅助
├── client/                        # 外部模型、OCR、文件服务等客户端
├── config/                        # Spring、OpenAPI、配置装配
└── shared/                        # 少量真正跨业务的通用类型
    └── api/
```

`agent/` 和 `knowledge/` 是核心业务上下文。如果出现独立管理界面、批处理任务或评测平台，也按业务上下文新增顶层业务包，而不是塞入泛化 `service`。

## 3. 分层规则

业务模块内部按需要使用以下轻量分层：

| 层 | 职责 | 示例 |
| --- | --- | --- |
| `api` | 协议入口、请求响应 DTO、参数校验和响应封装 | `AgentRunController`, `AgentRunRequest` |
| `application` | 一个明确业务用例的编排，不写底层 SQL | `AgentRunApplicationService` |
| `domain` 或业务子包 | 领域模型、领域接口、策略接口和值对象 | `RequestPlanner`, `RiskAssessor` |
| 业务能力子包 | 按能力聚合接口和实现 | `planning`, `retrieval`, `safety`, `answer` |

不强制每个模块都创建完整四层。目录应随真实复杂度出现，禁止为了“架构完整”创建空包。

Agent 外部协议入口只接收用户自然语言问题。任务主题、检索范围、查询改写、意图和风险信号属于 Agent 内部上下文，由 `RequestPlanner` 生成并在后续工具间传递，不作为用户必须提供的 API 参数。

## 4. 存储层边界

存储层是唯一建议横向独立管理的技术层：

```text
SQLite
  -> persistence/mapper
  -> persistence/entity
  -> persistence/store
  -> Agent / Knowledge 中定义的 Store、Repository、Reader、Writer 接口
```

规则：

- Entity 表达数据库表结构，不表达业务流程。
- Mapper 只做 MyBatis Plus 数据访问，不进入业务模块。
- Store / Repository / Reader / Writer 的接口属于业务上下文，数据库实现属于 `persistence/store`。
- 业务层不得直接依赖 Mapper，不拼接 SQL。
- JSON 快照字段只用于审计和调试；高频查询字段应拆为标准列。

示例：

```text
agent/runtime/AgentRunStore.java
persistence/store/agent/SqliteAgentRunStore.java

knowledge/graph/KnowledgeGraphReader.java
knowledge/graph/KnowledgeGraphWriter.java
persistence/store/knowledge/SqliteKnowledgeGraphStore.java
```

## 5. 包归属规则

代码按以下归属放置：

| 类型 | 目标位置 | 说明 |
| --- | --- | --- |
| 请求规划接口和实现 | `agent/planning` | Agent 专属请求规划能力 |
| 风险评估接口和实现 | `agent/safety` | Agent 专属风险评估能力 |
| 回答生成接口和实现 | `agent/answer` | Agent 专属回答生成能力 |
| Agent 证据召回接口 | `agent/retrieval` | Agent 组织问答流程时使用的召回能力 |
| 知识证据模型和仓储接口 | `knowledge/evidence` | 证据片段、检索模型、知识证据读写接口 |
| 知识图谱模型和读写接口 | `knowledge/graph` | 图谱节点、边、读写接口 |
| PDF/说明书入库流程 | `knowledge/ingestion` | 解析、切片、图谱写入等入库用例 |
| Agent HTTP 入口和 API DTO | `agent/api` | Controller、Request、Response、Event |
| 通用 API 包装 | `shared/api` | 只放真正跨业务复用的响应包装 |
| MyBatis Plus Mapper | `persistence/mapper` | 存储技术细节统一管理 |
| MyBatis Plus Entity | `persistence/entity` | 数据库表结构 |
| SQLite Store 实现 | `persistence/store/<business>` | 接口留在业务模块，数据库实现进存储层 |
| SQLite schema 初始化 | `persistence/schema` | schema 创建和迁移辅助 |

## 6. 判断一个类放哪里的规则

1. 先问“它服务哪个业务能力”：医学问答放 `agent`，知识图谱和说明书证据放 `knowledge`，PDF 入库放 `knowledge/ingestion`。
2. 再问“它是不是数据库技术细节”：Entity、Mapper、SQLite Store 实现和 schema 初始化放 `persistence`。
3. 如果只被 Agent 调用，优先放 `agent`，不要放通用 `service`。
4. 如果会被多个业务上下文复用，优先抽成明确业务上下文的接口，例如 `knowledge/evidence`，不要抽象成泛化 `service`。
5. `shared` 只放真正无业务归属且跨多个上下文复用的类型，例如统一响应包装、通用异常基类。

## 7. 禁止和谨慎事项

- 禁止新增泛化 `service/impl` 作为长期业务实现目录。
- 禁止主代码继续使用 `Demo`、`Mock`、`Temp` 命名。
- 禁止 Controller 直接调用 Mapper、工具实现或外部模型客户端。
- 禁止为了 DDD 形式感创建空的 `domain/application/infrastructure` 目录。
- 谨慎抽公共模块；只有第二个真实业务上下文复用时再抽。

## 8. 落地顺序

1. 先清理泛化 `service`：planning、retrieval、safety、answer 收敛进 Agent 或 Knowledge。
2. 再整理存储实现：接口保留在业务模块，SQLite 实现移动到 `persistence/store`。
3. 最后整理 API：Controller 和 DTO 按业务模块归档，保留少量 `shared/api`。
4. 每一步后运行相关测试，并使用 CodeGraph 检查调用影响范围。

本文是项目当前唯一架构设计文档。新增架构设计必须直接更新本文或替换本文，不保留过期设计文档。

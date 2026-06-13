# MedNoteAgent

MedNoteAgent 是一个基于 Spring Boot 的药品说明书知识管理与医学问答 Agent 项目。项目目标是将药品说明书转化为可检索、可追溯、可推理的证据知识，并基于说明书证据回答用药相关问题。

当前项目按正式开发组织，不再以 demo 作为代码和文档边界。功能可以先保持轻量，但模块命名、接口设计、存储层和测试都应服务于后续扩展。

## 当前技术基线

| 方向 | 当前选择 |
| --- | --- |
| 应用框架 | Spring Boot |
| 存储方案 | SQLite |
| 存储层框架 | MyBatis Plus |
| Agent 形态 | 单 Agent 编排 + 可插拔工具能力 |
| 运行审计 | SQLite 持久化 `agent_runs`、`agent_steps`、`agent_tool_calls` |
| 知识图谱 | SQLite 表模拟节点和关系，后续通过接口替换存储实现 |

## 快速启动

启动服务：

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' spring-boot:run
```

运行测试：

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' test
```

常用本地地址：

| 页面 | 地址 |
| --- | --- |
| Scalar API 文档 | http://localhost:8080/docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 健康测试接口 | http://localhost:8080/api/test/ping |

## 文档入口

所有项目文档统一维护在 [docs](docs/README.md) 目录下。日常开发优先阅读：

| 文档 | 说明 |
| --- | --- |
| [项目需求](docs/requirements/project-requirements.md) | 项目目标、功能范围、风险和验收标准 |
| [Agent 框架设计](docs/architecture/agent-framework-design.md) | 当前 Agent 架构、模块边界和扩展点 |
| [开发规范](docs/guides/development-guide.md) | 包结构、命名、接口、存储层和文档规范 |
| [Agent 并发与 SQLite 存储](docs/architecture/agent-concurrency-sqlite-design.md) | Agent 执行、审计表和 MyBatis Plus 存储边界 |
| [SQLite 知识图谱设计](docs/architecture/sqlite-knowledge-graph-design.md) | 图谱节点、边和读写接口 |
| [PDF 抽取与图谱设计](docs/architecture/pdf-extraction-graph-design.md) | 说明书入库、证据片段和图谱建模详情 |

## 当前 Agent 接口

> 说明：部分接口路径仍保留 `/api/demo-agent` 历史命名，后续可在不改变内部架构的前提下迁移为正式路径。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/test/ping` | 基础健康测试 |
| POST | `/api/test/echo` | JSON 请求和参数校验测试 |
| POST | `/api/demo-agent/run` | 同步运行 Agent |
| POST | `/api/demo-agent/see` | 返回 thought/tool/message 任务动态 |
| GET | `/api/demo-agent/tools` | 查看通过 `@AgentToolDefinition` 接入的工具 |
| GET | `/api/demo-agent/sessions` | 查看最近会话审计记录 |
| GET | `/api/demo-agent/sessions/{sessionId}` | 查看指定会话的工具调用审计记录 |
| GET | `/api/demo-agent/tool-call-failures` | 查看最近失败工具调用 |
| GET | `/api/demo-agent/stream` | 通过 SSE 流式运行 Agent |

## 架构摘要

Agent 相关能力应收敛在一个聚焦边界内：`agent` 负责编排、工具协议、运行时审计和动态调度；仅当能力需要被非 Agent 场景复用时，才抽到通用 `service` 或领域模块。

当前核心链路：

```text
AgentRunRequest
  -> MedNoteAgent
  -> request_planning
  -> drug_knowledge_search
  -> medical_risk_assessment
  -> answer_generation
  -> AgentRunStore(SQLite + MyBatis Plus)
  -> AgentRunResponse
```

工具实现可以先简单，但必须依赖接口，例如 `RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator`。后续替换模型、检索、风险策略或图谱存储时，不应修改 `MedNoteAgent` 的主编排结构。

模拟处理后的说明书数据位于 `src/main/resources/mock/processed-drug-data.json`，仅作为当前轻量能力的输入占位，不作为模块命名依据。评测用例位于 `src/test/resources/eval/agent-eval-cases.json`。

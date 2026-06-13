# MedNoteAgent

MedNoteAgent 是一个基于 Spring Boot 的药品说明书知识管理与医学问答 Agent 项目。项目目标是将药品说明书转化为可检索、可追溯、可推理的证据知识，并基于说明书证据回答用药相关问题。

项目按正式开发组织，不以 demo 作为代码和文档边界。功能可以先保持轻量，但模块命名、接口设计、存储层和测试都必须服务于长期演进。

## 当前技术基线

| 方向 | 选择 |
| --- | --- |
| 应用框架 | Spring Boot |
| 存储方案 | SQLite |
| 存储层框架 | MyBatis Plus |
| Agent 形态 | 单 Agent 编排 + 可插拔工具能力 |
| 运行审计 | SQLite 持久化 `agent_runs`、`agent_steps`、`agent_tool_calls` |
| 知识图谱 | 通过 `knowledge/graph` 领域接口管理节点和关系，SQLite 实现放入 `persistence/store/knowledge` |

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
| [业务模块化目录结构设计](docs/architecture/business-module-structure-design.md) | 最新目录结构、业务模块边界和存储层边界 |
| [开发规范](docs/guides/development-guide.md) | 包结构、命名、接口、存储层和文档规范 |
| [CodeGraph 使用指南](docs/guides/codegraph-guide.md) | 代码结构索引、符号查询和影响分析 |

## 当前 Agent 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/test/ping` | 基础健康测试 |
| POST | `/api/test/echo` | JSON 请求和参数校验测试 |
| POST | `/api/agent/run` | 同步运行 Agent |
| POST | `/api/agent/see` | 返回 thought/tool/message 任务动态 |
| GET | `/api/agent/tools` | 查看通过 `@AgentToolDefinition` 接入的工具 |
| GET | `/api/agent/sessions` | 查看最近会话审计记录 |
| GET | `/api/agent/sessions/{sessionId}` | 查看指定会话的工具调用审计记录 |
| GET | `/api/agent/tool-call-failures` | 查看最近失败工具调用 |
| GET | `/api/agent/stream` | 通过 SSE 流式运行 Agent |

## 架构摘要

代码按业务上下文组织。`agent` 负责医学问答入口、编排、工具协议、运行时审计和 Agent 专属能力；`knowledge` 负责说明书证据、知识图谱和入库流程；`persistence` 只承载 Entity、Mapper、SQLite Store 实现和 schema 初始化。

核心链路：

```text
AgentRunRequest
  -> agent/application
  -> request_planning
  -> drug_knowledge_search
  -> medical_risk_assessment
  -> answer_generation
  -> AgentRunStore
  -> persistence/store/agent
  -> AgentRunResponse
```

工具实现可以先简单，但必须依赖业务接口，例如 `RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator`。评测用例位于 `src/test/resources/eval/agent-eval-cases.json`。

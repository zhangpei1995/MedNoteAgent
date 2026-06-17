# MedNoteAgent

MedNoteAgent 是一个基于 Spring Boot 的药品说明书知识管理与医学问答 Agent 项目。项目目标是将药品说明书转化为可检索、可追溯、可推理的证据知识，并提供本地 demo agent 接口用于在线调试。

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

所有项目文档统一维护在 [docs](docs/README.md) 目录下。

| 分类 | 说明 |
| --- | --- |
| [项目需求](docs/requirements/project-requirements.md) | 项目目标、功能范围、风险和验收标准 |
| [Agent 框架设计](docs/architecture/agent-framework-design.md) | Agent 编排、模块边界和扩展点 |
| [架构优化计划](docs/architecture/architecture-optimization-plan.md) | 当前问题、目标架构和分阶段优化计划 |
| [项目结构 Review](docs/architecture/project-structure-review.md) | 目录职责、精简边界和设计模式对应关系 |
| [聚焦交付复盘](docs/architecture/focused-delivery-review.md) | 架构是否发散、职责是否清晰、演示和迭代如何持续 |
| [下一阶段任务](docs/architecture/next-stage-tasks.md) | P2/P3 任务拆解、验收标准和实施顺序 |
| [PDF 抽取与图谱设计](docs/architecture/pdf-extraction-graph-design.md) | PDF 入库、证据片段、图谱节点和关系 |
| [开发规范](docs/guides/development-guide.md) | 代码组织、包职责、命名和实现规范 |
| [Agent 协作手册](docs/guides/agent-handbook.md) | Agent 检索、职责边界、扩展方式和自检清单 |
| [本地 Demo 方案](docs/guides/local-demo-implementation.md) | 本地 demo 范围、存储设计和实现步骤 |
| [参考资料](docs/reference/drug-instructions/) | 药品说明书原始 PDF |

## 当前 Demo 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/test/ping` | 基础健康测试 |
| POST | `/api/test/echo` | JSON 请求和参数校验测试 |
| POST | `/api/demo-agent/run` | 同步运行本地 demo agent |
| POST | `/api/demo-agent/see` | 返回 thought/tool/message 任务动态，便于实现类似 GPT 对话的前端效果 |
| GET | `/api/demo-agent/tools` | 查看通过 `@AgentToolDefinition` 注解接入、可被 agent 剪枝选择的工具 |
| GET | `/api/demo-agent/sessions` | 查看最近会话审计记录 |
| GET | `/api/demo-agent/sessions/{sessionId}` | 查看本地内存中的会话级工具调用审计记录 |
| GET | `/api/demo-agent/tool-call-failures` | 查看最近失败工具调用 |
| GET | `/api/demo-agent/stream` | 通过 SSE 流式运行本地 demo agent |

## Agent 骨架说明

当前骨架通过 `AgentTool` 接口和 `@AgentToolDefinition` 注解接入自定义工具。关键词、意图、query 改写和说明书推荐统一收敛在 `request_planning`，整体只保留规划、检索、风控、生成四类工具；当前工具只保留 demo 级占位实现，后续可替换为真实模型、检索或安全策略实现。运行时由 `AgentToolRegistry` 读取工具注解中的阶段、顺序、名称、描述和触发词，根据任务文本打分剪枝，只加载并执行所需工具。`/api/demo-agent/see` 与 `/api/demo-agent/stream` 会返回 `thought`、`tool`、`message` 三类事件，前端可按事件增量渲染出类似 GPT 对话的任务动态。

模拟处理后的说明书数据位于 `src/main/resources/mock/processed-drug-data.json`，评测用例位于 `src/test/resources/eval/agent-eval-cases.json`。

千问客户端已预留流式方法：DashScope Java SDK 使用 `Generation#streamCall`，并设置 `incrementalOutput(true)` 以按 chunk 接收新增内容。

关键词流程：`request_planning` 统一生成任务关键词、意图、query keywords 和推荐说明书，后续动态工具选择使用这些字段。小模型建议配置为低温度、JSON 输出、8-16 个关键词，并通过 `mednote.agent.keyword.small-model` 管理 provider/model/temperature。

意图识别小模型建议使用低延迟、低温度、稳定 JSON 输出的模型，配置位于 `mednote.agent.intent.small-model`。输出应包含查询目标、用药风险等级、风险信号和推荐说明书，demo 中由 `request_planning` 工具以规则占位实现。

排查问题时优先查看 `/api/demo-agent/see` 返回的 `sessionId`、每个工具事件的 `metadata.toolCall`，或通过 `GET /api/demo-agent/sessions/{sessionId}` / `GET /api/demo-agent/tool-call-failures` 查询会话审计和失败调用；工具会在每轮执行后重新动态选择，决策包含候选、跳过、卸载、停止原因、置信度和人工复核标记，已执行工具会在本会话内卸载避免重复调用。


## 评测与演示产出

当前 demo 的产出路径是：先用 `/api/demo-agent/see` 或 `/api/demo-agent/stream` 展示 thought/tool/message 动态，再用 `/api/demo-agent/sessions/{sessionId}` 复盘工具调用链；质量回归通过 `AgentEvalRunnerTest` 读取 `src/test/resources/eval/agent-eval-cases.json`，检查工具链、证据命中、风险等级、推荐说明书和回答关键短语。

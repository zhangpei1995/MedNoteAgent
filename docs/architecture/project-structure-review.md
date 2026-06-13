# 项目结构 Review 与精简建议

> 状态：历史结构评审。当前包结构和命名规范以 [开发规范](../guides/development-guide.md) 为准。

本文用于约束当前 demo 阶段的目录职责，避免 Agent 骨架继续膨胀成“万能类”。

## 当前结论

整体结构可以保留：`agent` 负责编排，`agent.runtime` 承载会话级记录、工具调用审计和动态工具选择，`agent.tool` 承载可替换步骤，`service` 提供薄能力，`client` 封装外部模型 SDK，`controller` 只暴露 HTTP 接口，`dto` 只做传输对象，`domain` 放领域值对象。

本轮整理已处理的冗余：

- 移除了 `QianwenClient` 中面向手工调试的 `main` 方法，避免生产客户端混入测试入口。
- 为主要包补充 `package-info.java`，明确每个目录职责。
- 精简 `AgentToolRegistry` 的工具描述计算，避免同一工具在一次打分中重复解析注解。
- 将关键词、意图、query 改写、说明书推荐合并为 `request_planning` 工具，避免“一个小功能一个工具”。
- 新增 `agent.runtime` 模块，集中处理会话级信息、工具调用记录、动态追加/卸载工具。
- 为工具核心接口和上下文对象补充职责注释。

## 目录职责

```text
src/main/java/org/med/note
├── agent/          # 单次 Agent 请求编排；不写具体意图、检索、生成规则
├── agent/runtime/  # 会话、工具调用记录、动态工具选择
├── agent/tool/     # 可插拔工具协议与 demo 工具实现
├── client/         # 外部模型或服务 SDK 封装
├── config/         # Spring、OpenAPI、环境变量配置
├── controller/     # HTTP 入参、出参、SSE 输出；不承载业务流程
├── domain/         # 领域值对象
├── dto/            # API 传输对象
└── service/        # 可复用薄能力，被工具调用
```

## 设计模式对应关系

| 设计点 | 当前落点 | 说明 |
| --- | --- | --- |
| Strategy / Command | `AgentTool` | 每个工具是一个可替换执行单元 |
| Registry | `AgentToolRegistry` | 发现、描述、打分、候选排序 |
| Planner | `AgentToolPlanner` | 会话内动态追加/卸载工具 |
| Pipeline / Chain | `MedNoteAgent` | 按工具顺序推进上下文 |
| Audit Log | `AgentSession` / `ToolCallRecord` | 记录会话级工具调用，便于排查问题 |
| DTO | `dto` records | API 入出参结构化 |
| Adapter | `QianwenClient` | 屏蔽 DashScope SDK 细节 |
| Value Object | `EvidenceChunk` | 表达证据片段 |

## 后续精简边界

- 工具粒度以“可替换能力”而不是“单个函数”为边界；关键词、意图、query 改写这类强耦合规划能力应合并为一个 planning 工具。
- 如果某个 demo tool 超过约 200 行，应拆为 `strategy` 或专门 service，不继续塞进 tool 类。
- `agent.tool` 当前为了 demo 方便保持单包；当工具数量继续增加时，再拆分为 `agent.tool.core` 与 `agent.tool.demo`。
- `MockDrugKnowledgeBase` 只允许保留小规模 mock 数据；真实入库、向量召回和重排应迁移到 repository/service 组合。
- `MedNoteAgent` 不应出现医学关键词规则、说明书章节规则或 prompt 细节。
- Controller 只做协议转换和 SSE 发送，不直接调用工具或模型客户端。
- 排查问题优先查看最终事件中的 `sessionId`、`toolCalls` 和每个工具事件的 `metadata.toolCall`。

## 文档完整性要求

新增工具时需要同步更新：

1. `README.md` 的 demo 能力说明。
2. `docs/architecture/agent-framework-design.md` 的流程或模型选择说明。
3. `src/test/resources/eval/agent-eval-cases.json` 的预期工具链或关键字段。
4. 对外接口变化时更新 controller 测试。

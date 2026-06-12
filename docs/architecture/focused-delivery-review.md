# 聚焦交付与可扩展性复盘

本文是对当前 Agent 架构的自我审查，目的不是继续堆功能，而是确认代码是否服务于项目目标：药品说明书证据问答、可观察的工具链、可替换的模型/检索/风控实现，以及可持续评测。

## 1. 项目真正要解决的问题

MedNoteAgent 当前阶段不是要做复杂多 Agent 系统，而是要建立一条可演示、可排查、可替换、可评测的说明书问答链路：

1. 用户提出说明书相关问题。
2. request planning 识别查询目标、风险信号、推荐说明书和检索关键词。
3. retrieval 返回可追溯证据。
4. safety 给出用药风险等级。
5. generation 只基于证据生成回答。
6. runtime 记录每一步为什么执行、输入是什么、输出是什么、失败如何降级。

因此架构优先级是：边界清晰 > 可观测 > 可替换 > 评测闭环 > 再接真实模型和知识库。

## 2. 是否发散

### 已收敛的部分

- 没有继续拆出关键词工具、意图工具、query 改写工具等细粒度工具，而是合并为 `request_planning`。
- Agent 工具保持四类：planning、retrieval、safety、generation。
- 真实逻辑下沉到 service SPI，工具只做能力入口和上下文转换。
- session、工具选择、工具调用记录集中放在 `agent.runtime`，避免散落到 controller 或 service。

### 仍需警惕的发散点

- `DemoRequestPlanner` 规则已经偏长，后续不要继续堆规则；真实实现应换成小模型 JSON schema + 规则兜底。
- `AgentToolPlanner` 当前仍是启发式 planner，不能过早包装成“智能调度系统”；下一步只补可解释评分和终止策略。
- 文档数量增加后要保持入口清晰，避免文档之间互相重复。

## 3. 职责是否清晰

| 模块 | 当前职责 | 不应承担 |
| --- | --- | --- |
| `controller` | HTTP/SSE 暴露、参数转发 | 医学规则、工具选择、模型调用细节 |
| `agent` | 请求级编排、merge、事件输出 | 检索算法、风控规则、prompt 细节 |
| `agent.runtime` | session、planner、审计、错误状态 | 药品知识、业务规则 |
| `agent.tool` | 工具契约、注解、上下文转换、工具入口 | 大量业务实现 |
| `service.spi` | 可替换能力接口 | demo 规则 |
| `service.impl` | 当前 demo 占位实现 | 长期生产策略绑定 |
| `test/eval` | 质量回归和演示验收 | 线上业务逻辑 |

当前职责基本清晰；后续若某个文件超过明显职责，应优先拆 service 内部策略，而不是新增 Agent 工具。

## 4. 是否冗余

当前保留的抽象是必要的：

- `AgentTool` / `AgentToolDefinition`：用于接入和描述工具。
- `AgentToolRegistry`：用于发现和基础排序。
- `AgentToolPlanner`：用于会话级动态选择、卸载、跳过、停止原因。
- `AgentRunStore`：用于审计查询和问题排查。
- `service.spi`：用于替换模型、检索、风控和生成实现。

暂不建议继续增加：

- 多 Agent 管理器。
- 每个小步骤一个工具。
- 复杂 workflow DSL。
- 复杂持久化模型。

这些都会让 demo 阶段偏离核心目标。

## 5. 设计模式和范式

- Strategy：`RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator` 是可替换策略接口。
- Registry：`AgentToolRegistry` 管理工具发现和描述。
- Planner / Policy：`AgentToolPlanner` 管理动态选择策略。
- Audit Log：`ToolCallRecord` 和 `AgentRunRecord` 形成可复盘记录。
- DTO / Record：上下文、结果、事件优先使用不可变 record，减少隐式副作用。

这些模式目前是服务于边界和可替换性的，不是为了形式化套模式。

## 6. 演示效果如何产出

最小演示路径：

1. 调用 `POST /api/demo-agent/see` 展示 GPT 风格 thought/tool/message 动态。
2. 前端按 `eventType` 渲染工具选择、工具执行和最终回答。
3. 从事件 metadata 取 `sessionId`。
4. 调用 `GET /api/demo-agent/sessions/{sessionId}` 展示工具调用审计。
5. 如果出现异常，调用 `GET /api/demo-agent/tool-call-failures` 查看失败工具。
6. 本地/CI 运行 eval 测试，输出工具链、证据、风险和回答是否退化。

## 7. 后续优先级

P0/P1/P2 最小闭环已经建立。后续优先级如下：

1. 增加更多 eval case，覆盖常见药品章节和高风险问题。
2. 将 `RequestPlanner` 接入小模型 JSON schema，并保留规则兜底。
3. 将 `EvidenceRetriever` 替换为关键词 + 向量 + rerank 的混合检索。
4. 将风险策略拆成规则通道和模型通道，并明确高风险拦截/复核策略。
5. 将 `AgentRunStore` 替换为数据库或日志平台持久化。

## 8. 当前结论

当前架构没有明显过度离散，核心职责已经聚焦在“可演示、可排查、可替换、可评测”的 Agent 骨架上。下一阶段不要继续扩工具数量，应优先扩评测样本、替换 SPI 实现，并用 session audit 验证每次迭代是否真的提升了效果。

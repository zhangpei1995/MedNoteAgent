# 下一阶段任务清单

> 状态：历史任务拆解。任务状态需结合当前代码复核；当前开发优先级以 [Agent 框架设计](agent-framework-design.md)、[开发规范](../guides/development-guide.md) 和最新 issue/需求为准。

本文承接 P0/P1 架构收敛结果，列出下一阶段应优先推进的任务。原则：不扩工具数量，先把可替换接口、会话可观测性、错误模型和评测闭环打牢。

## 已完成的 P0/P1 收敛

- 工具粒度收敛为规划、检索、风控、生成四类能力。
- 新增 `request_planning`，合并关键词、意图、query 改写和说明书推荐。
- 新增 `agent.runtime`，集中处理 session、动态工具选择和工具调用记录。
- 新增 `AgentRunStore` 内存实现与 session 查询接口。
- 工具调用支持 `completed` / `failed` 状态记录，失败会进入 `ToolCallRecord`。
- 工具依赖 service SPI 接口，demo 实现已移动到 `service.impl`。
- Planner 决策已输出 `skippedTools`、`stopReason`、`confidence` 和 `requiresHumanReview`。
- `AgentRunStore` 已支持最近会话查询、失败工具调用查询和内存上限。
- 已增加 eval runner 测试，读取 `agent-eval-cases.json` 做工具链、证据、风险和回答检查。

## P2-A：接口测试与实现命名细化

已完成：工具只依赖 SPI，不直接依赖 demo 类；新增 SPI contract 测试覆盖 planning、retrieval、safety、generation 的最小可用链路。后续如实现增多，再从 `service.impl` 细分到 `planning.impl`、`retrieval.impl`、`safety.impl`、`generation.impl`，不要提前制造目录层级。

验收：替换任一 demo 实现不需要修改 `agent`、`agent.tool`、`controller`。

## P2-B：AgentRunStore 强化

已完成：最近 N 条查询、失败工具调用查询和内存上限。剩余工作：将 session 返回结构稳定为专用调试 DTO，并在持久化前确认字段兼容性。

验收：可以用 sessionId 复盘一次完整 Agent 执行链路。

## P2-C：Planner 策略增强

已完成：`requiresHumanReview`、启发式 confidence、跳过/停止原因输出。剩余工作：支持受控的工具重复执行策略；高风险或证据不足时允许 planner 终止生成或强制安全工具。

验收：每次跳过、追加、终止都有可解释原因。

## P2-D：错误模型与降级

已完成：`ToolExecutionStatus`、`ToolFailureType` 和失败工具调用记录。剩余工作：让 `MedNoteAgent` 根据错误类型决定继续、跳过、降级或终止，并在前端事件展示降级原因。

验收：任意工具失败不会让接口 500，且用户能看到可解释的降级结果。

## P2-E：评测闭环

已完成：增加 eval runner 测试，读取 `agent-eval-cases.json`，检查工具链、证据 ID、风险等级、推荐说明书和回答关键短语，并输出基础 passRate。剩余工作：在 CI 中单独暴露 eval 命令，并随 case 增多输出分项指标。

验收：新增 case 后可以量化判断架构改动是否退化。

## P3：下一阶段重点

1. 扩充 eval case 至 20 条以上，覆盖章节查询、禁忌、特殊人群、不良反应、用法用量。
2. `RequestPlanner` 接入小模型 JSON schema，保留当前规则兜底。
3. `EvidenceRetriever` 接入混合检索，并将 mock evidence 替换为真实处理后数据。
4. 完成工具失败后的继续/跳过/降级/终止策略。
5. 将 session audit 从内存迁移到持久化存储。

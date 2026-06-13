# 架构批判性 Review 与优化计划

> 状态：历史复盘。当前架构真相源为 [Agent 框架设计](agent-framework-design.md) 和 [开发规范](../guides/development-guide.md)。若本文与当前主线文档或代码冲突，以当前主线文档和代码为准。

本文以批判视角评审当前 MedNoteAgent 架构，重点回答：现在的问题是什么、哪些设计需要收敛、后续按什么顺序优化。

## 1. 总体判断

当前架构已经具备 demo 骨架：有 Agent 编排、可插拔工具、动态工具选择、会话级工具调用记录、SSE 事件和 mock 数据。但它仍然是“演示型架构”，距离可长期演进的工程架构还有差距。

核心风险不是功能缺失，而是边界继续膨胀：如果继续把规划、检索、风控、生成、评测都放在同一层，很快会变成新的单体 Agent。后续优化应优先做“边界收敛”和“可观测闭环”，再扩展真实模型和知识库。

## 2. 当前主要问题

### 2.1 工具边界仍需稳定

已将关键词、意图、query 改写合并成 `request_planning`，这是正确方向。但 `request_planning` 内部仍然包含较多 demo 规则，后续如果继续增加药品推荐、问法改写、人群识别等规则，会再次膨胀。

建议：保持 `RequestPlanningTool` 轻薄，把真实逻辑放到 planning service 或 strategy；工具只作为能力入口和上下文转换器。

### 2.2 runtime 已有内存审计，但仍缺生产级持久化

当前 `AgentSession`、`ToolCallRecord` 和 `AgentRunStore` 已能记录单次运行、按 sessionId 查询、查看最近会话和失败工具调用。剩余问题是这些记录仍在内存中，不能跨进程留存，也没有指标聚合。

建议：demo 阶段继续保留内存实现；生产化前再替换为数据库或日志系统，不要过早引入复杂存储。

### 2.3 动态工具选择过于简单

当前 `AgentToolPlanner` 是“每轮重新排序 + 排除已执行工具”的轻量实现。它能演示追加/卸载，但还不能表达：为什么跳过某个工具、是否需要重复执行某个工具、是否需要回退、是否需要终止。

建议：引入更明确的 planning policy，输出 `selected`、`skipped`、`stopReason`、`confidence`、`requiresHumanReview` 等字段。

### 2.4 service 层已接口化，但 demo 实现仍需克制

`RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator` 已抽为 SPI，demo 实现已放到 `service.impl`。当前风险不再是替换困难，而是 demo 实现继续堆规则导致新单体。

建议：后续真实能力通过替换 SPI 实现完成，不要把复杂业务继续写进工具或 controller。

### 2.5 错误模型有基础，但降级策略还薄

`ToolExecutionStatus` 和 `ToolFailureType` 已能表达工具成功/失败和粗粒度失败类型。剩余问题是失败后的继续、跳过、降级或终止策略仍然简单。

建议：下一步只补少量明确策略，例如模型超时可降级模板回答、检索为空可要求补充药品名、高风险可强制人工复核。

### 2.6 评测闭环已起步，但样本不足

当前已有 JSON fixture 和 eval runner 测试，能检查工具链、证据、风险、推荐说明书和回答关键短语。主要不足是 case 数量太少，还不能覆盖多药品、多章节、多风险场景。

建议：优先扩充 eval case，而不是继续增加架构抽象。

## 3. 推荐目标架构

```text
controller
  └── agent/MedNoteAgent                  # 请求级编排，不写业务细节
      ├── runtime/AgentToolPlanner        # 动态工具选择、追加、卸载、终止判断
      ├── runtime/AgentRunStore           # 会话记录存储与查询
      └── tool/*                          # 可替换能力入口
          ├── RequestPlanningTool
          ├── DrugKnowledgeSearchTool
          ├── MedicalRiskAssessmentTool
          └── AnswerGenerationTool

service
  ├── spi/*                               # RequestPlanner / EvidenceRetriever / RiskAssessor / AnswerGenerator
  └── impl/*                              # 当前 demo 实现，后续可替换为真实模型、检索和风控实现
```

## 4. 分阶段优化计划

### P0：立即收敛（当前 demo 稳定前）

目标：避免架构继续发散，让排查路径清晰。

1. 保持工具数量少而聚焦：规划、检索、风控、生成四类即可。
2. `MedNoteAgent` 只保留编排、merge 和事件输出，不增加医学规则。
3. 所有工具事件必须带 `sessionId` 和 `toolCall`。
4. 文档中明确：新增工具必须说明“为什么不能合并到已有工具”。
5. 修正 PR 描述和测试说明，不宣称未实际跑通的 `mvn test`。

验收标准：`/api/demo-agent/see` 能看清完整工具链和每次工具输入/输出摘要。

### P1：短期优化（1-2 个迭代）

目标：把 demo 架构变成可替换架构。

1. 新增接口：`RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator`。
2. 将当前 demo service 移到 demo/impl 实现，工具只依赖接口。
3. 新增 `AgentRunStore`，先实现内存版，支持按 `sessionId` 查询工具调用记录。
4. 工具异常统一进入 `ToolCallRecord.status=FAILED`，并记录错误类型和错误摘要。
5. Planner 输出更丰富的决策原因，包括 skipped tools 和 stop reason。

验收标准：替换任意一个 demo service 不需要修改 `MedNoteAgent` 和 controller。

### P2：中期优化（3-5 个迭代）

目标：接入真实知识库和小模型，同时保持可观测性。

1. `RequestPlanner` 接入小模型 JSON schema，保留规则兜底。
2. `EvidenceRetriever` 接入混合检索：关键词 + 向量 + rerank。
3. 入库侧生成 `knowledgeKeywords`、章节标签、人群标签和风险标签。
4. 风控策略独立为规则 + 模型双通道，高风险时强制安全提示。
5. Eval runner 自动跑 fixture，输出工具链、证据、风险、回答四类指标。

验收标准：新增 20 条评测 case 后，能够自动输出质量报告。

### P3：长期优化（生产化前）

目标：可靠性、审计和治理。

1. 会话和工具调用持久化到数据库或日志平台。
2. 对模型调用、检索调用增加 timeout、retry、circuit breaker。
3. 增加权限、脱敏和医学免责声明策略。
4. 引入 prompt/template 版本管理。
5. 建立线上指标：工具失败率、召回为空率、高风险占比、平均延迟、用户追问率。

验收标准：任意线上回答都能追溯到 session、工具链、证据、prompt 版本和模型版本。

## 5. 当前不建议做的事

- 不建议继续增加很多细粒度工具，例如单独的“孕妇识别工具”“关键词工具”“章节工具”。这些应归入 planning 或 safety 能力。
- 不建议过早引入复杂多 Agent。当前问题主要是工具边界和可观测性，不是 Agent 数量不足。
- 不建议把真实向量库接入写死在工具里，应先定义检索接口。
- 不建议让 controller 暴露内部工具参数，controller 只应暴露任务输入和事件输出。

## 6. 下一步最小行动清单

1. 增加 eval runner，读取 `src/test/resources/eval/agent-eval-cases.json` 并输出指标。
2. 将 `ToolSelectionDecision.confidence` 从固定启发值升级为可解释评分。
3. 增加 session 列表、失败过滤和内存过期策略。
4. 细化工具失败后的继续、跳过、降级或终止策略。

## 7. P0/P1 处理结果

本轮已完成 P0/P1 中最关键的架构搭建事项：

- 工具粒度收敛：将关键词、意图、query 改写和说明书推荐合并为 `request_planning`。
- 可替换接口：新增 `RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator` SPI，工具依赖接口而不是 demo 实现，demo 实现放到 `service.impl`。
- 会话查询：新增 `AgentRunStore` 和内存实现，并提供按 sessionId 查询接口。
- 错误记录：工具异常会写入 `ToolCallRecord.status=FAILED`，包含错误类型和错误摘要。
- 动态选择：`AgentToolPlanner` 每轮基于最新上下文重排候选工具，已执行工具会在本会话卸载，决策结果包含候选、跳过、停止原因和置信度。

剩余工作已拆到 [下一阶段任务](next-stage-tasks.md)。

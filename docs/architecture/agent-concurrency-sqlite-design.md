# Agent 并发执行与 SQLite 存储设计

## 1. 背景

当前 Agent 已经具备工具注册、动态选择、工具调用审计和会话查询能力。原执行模型是每轮只选择并执行一个工具：

```text
request_planning
  -> drug_knowledge_search
  -> medical_risk_assessment
  -> answer_generation
```

这个模型简单可靠，但当检索、风险评估、外部模型调用都变成真实远程调用后，请求耗时会线性叠加。为了降低端到端耗时，当前设计引入依赖感知的批量计划与并发执行，并把 Agent 运行记录持久化到 SQLite。

## 2. 设计目标

1. **计划后并发**：`request_planning` 产出任务理解后，允许依赖已满足、互不依赖的工具在同一轮并发执行。
2. **依赖显式化**：每个工具通过注解声明 `dependsOn` 与 `parallelizable`，调度器不隐式猜测工具关系。
3. **结果稳定可审计**：并发工具完成后，仍按 planner 决定的工具顺序合并结果和生成审计记录。
4. **接口兼容**：`AgentRunResponse`、`AgentStep`、`AgentRunRecord` 等外部响应结构保持不变，仅在 step metadata 中补充并发相关字段。
5. **SQLite + MyBatis Plus**：当前使用 SQLite 文件存储，通过 MyBatis Plus Mapper 访问 run、step、tool_call 三表，后续可在保持上层接口不变的前提下迁移 MySQL。

## 3. 并发执行模型

### 3.1 工具元数据

`@AgentToolDefinition` 新增两个字段：

```java
String[] dependsOn() default {};
boolean parallelizable() default true;
```

含义：

- `dependsOn`：当前工具启动前必须完成的工具名列表。
- `parallelizable`：当前工具是否允许与其他已就绪工具共享同一份 `ToolContext` 快照并发执行。

当前工具依赖关系：

| 工具 | 依赖 | 是否可并发 | 说明 |
| --- | --- | --- | --- |
| `request_planning` | 无 | 是 | 首轮执行，产出 intent、queryKeywords、风险初判等 |
| `drug_knowledge_search` | `request_planning` | 是 | 计划完成后检索证据 |
| `medical_risk_assessment` | `request_planning` | 是 | 计划完成后做快速安全评估 |
| `answer_generation` | `drug_knowledge_search`, `medical_risk_assessment` | 否 | 必须等待证据和风险结果后生成最终回答 |

### 3.2 Planner 批次生成

`AgentToolPlanner` 新增 `selectNextBatch`：

```text
输入：ToolContext + 已执行工具集合
  -> registry 排名候选工具
  -> 过滤已执行工具
  -> 过滤依赖未满足工具
  -> 如果第一个 ready 工具可并发，则收集同批 ready 且 parallelizable 的工具
  -> 返回 ToolSelectionBatch
```

`ToolSelectionBatch` 会记录：

- `selectedTools`：本轮要执行的工具列表。
- `candidateTools`：registry 排名后的候选工具名。
- `unloadedTools`：本 session 已执行、不会再次执行的工具。
- `parallel`：本批是否包含多个工具。
- `reason`、`stopReason`、`confidence`、`requiresHumanReview`：继续用于 UI 与审计。

### 3.3 Agent 执行策略

`MedNoteAgent` 每轮执行流程：

```text
selectNextBatch
  -> 写入 tool_selection thought
  -> 对当前 ToolContext 做快照
  -> 同批工具通过固定线程池并发执行
  -> 等待本批全部完成
  -> 按 tool_order 排序
  -> 逐个记录 ToolCallRecord
  -> 成功结果按顺序 merge 回上下文
  -> 有失败或 answer_generation 完成则结束循环
```

上下文合并仍采用原规则：

- 工具返回空字段表示保留旧上下文。
- evidence、riskLevel、finalAnswer 等按工具结果逐步覆盖。
- `memory` 记录每个工具的 metadata，便于排查。

### 3.4 并发安全边界

并发工具拿到的是同一个逻辑时刻的 `ToolContext` 快照，不应修改共享状态。当前工具只读取上下文并返回 `ToolResult`，写入统一由 agent merge 完成。

后续如果新增工具，需要遵循：

1. 不在 `execute` 中修改 `ToolContext.memory()`。
2. 如果必须依赖另一个工具的输出，通过 `dependsOn` 声明依赖。
3. 对外部系统有副作用的工具应设置 `parallelizable = false`，除非具备幂等和并发控制。

## 4. SQLite 存储设计

### 4.1 配置

默认配置：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${mednote.agent.store.sqlite.path:data/mednote-agent.db}
    driver-class-name: org.sqlite.JDBC

mednote:
  agent:
    session:
      max-records: 100
    executor:
      parallelism: 4
    store:
      sqlite:
        path: data/mednote-agent.db
```

`data/*.db` 属于运行时产物，已加入 `.gitignore`。启动时 `SqliteSchemaInitializer` 负责创建目录和表，`SqliteAgentRunStore` 通过 MyBatis Plus Mapper 读写数据。

### 4.1.1 存储层边界

```text
AgentRunStore
  -> SqliteAgentRunStore
      -> AgentRunMapper
      -> AgentStepMapper
      -> AgentToolCallMapper
          -> SQLite
```

约束：

1. `MedNoteAgent` 只依赖 `AgentRunStore`。
2. `SqliteAgentRunStore` 负责 record 与 entity 转换。
3. Mapper 只放在 `dao` 包，不进入业务编排层。
4. 表结构变化时同步更新 entity、mapper、schema 初始化和测试。

### 4.2 表结构

```text
agent_runs
  session_id          VARCHAR(64) PRIMARY KEY
  started_at          VARCHAR(40)
  finished_at         VARCHAR(40)
  tool_call_count     INTEGER
  step_count          INTEGER
  payload_json        TEXT
  created_at          VARCHAR(40)

agent_steps
  id                  INTEGER PRIMARY KEY AUTOINCREMENT
  session_id          VARCHAR(64)
  step_order          INTEGER
  stage               VARCHAR(120)
  event_type          VARCHAR(40)
  status              VARCHAR(40)
  content             TEXT
  metadata_json       TEXT
  created_at          VARCHAR(40)

agent_tool_calls
  id                  INTEGER PRIMARY KEY AUTOINCREMENT
  session_id          VARCHAR(64)
  tool_order          INTEGER
  tool_name           VARCHAR(120)
  phase               VARCHAR(80)
  status              VARCHAR(40)
  started_at          VARCHAR(40)
  finished_at         VARCHAR(40)
  duration_ms         INTEGER
  summary             TEXT
  input_snapshot_json TEXT
  output_metadata_json TEXT
  failure_type        VARCHAR(80)
  error_type          VARCHAR(160)
  error_message       TEXT
  payload_json        TEXT
```

索引：

```text
idx_agent_runs_finished_at
idx_agent_tool_calls_status_finished_at
```

### 4.3 JSON 快照策略

表中既保存可查询字段，也保存完整 JSON：

- `agent_runs.payload_json`：完整 `AgentRunRecord`。
- `agent_steps.metadata_json`：单步扩展信息。
- `agent_tool_calls.payload_json`：完整 `ToolCallRecord`。
- `agent_tool_calls.input_snapshot_json` / `output_metadata_json`：便于后续按工具输入输出排查。

这样做的原因：

1. 当前字段仍会随正式功能演进。
2. API 读取可以直接恢复现有 record，降低兼容成本。
3. 后续迁 MySQL 时，可先保留 JSON 字段，再逐步把高频查询字段拆成标准列。

### 4.4 数据保留

`mednote.agent.session.max-records` 控制保留最近 N 条 run。保存新 session 后，store 会按 `finished_at` 保留最新记录，删除更早 session 的 run、step、tool_call。

## 5. 迁移到 MySQL 的建议

SQLite 是当前正式存储方案；迁 MySQL 时建议：

1. 把 `VARCHAR(40)` 时间字段改为 `DATETIME(3)` 或 `TIMESTAMP(3)`。
2. 把 `TEXT` JSON 字段改为 `JSON` 类型。
3. 给 `agent_steps(session_id, step_order)` 和 `agent_tool_calls(session_id, tool_order)` 增加唯一索引。
4. 给外键增加 `ON DELETE CASCADE`，简化保留策略删除逻辑。
5. 新增 `MysqlAgentRunStore` 或通用关系型实现，保持 `AgentRunStore` 接口不变。

## 6. 验证结果

已执行：

```text
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' test
```

结果：

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

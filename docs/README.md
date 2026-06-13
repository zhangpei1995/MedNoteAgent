# MedNoteAgent 文档中心

本目录是 MedNoteAgent 项目需求、架构、开发规范和参考资料的统一入口。文档职责应像代码职责一样清晰：入口文档只做索引，设计文档说明边界和决策，详情文档再展开表结构、流程和边界条件。

## 当前文档结构

```text
docs/
├── README.md
├── requirements/
│   └── project-requirements.md
├── architecture/
│   ├── agent-framework-design.md
│   ├── agent-concurrency-sqlite-design.md
│   ├── sqlite-knowledge-graph-design.md
│   ├── pdf-extraction-graph-design.md
│   ├── architecture-optimization-plan.md
│   ├── focused-delivery-review.md
│   ├── next-stage-tasks.md
│   └── project-structure-review.md
├── guides/
│   ├── development-guide.md
│   ├── codegraph-guide.md
│   └── local-demo-implementation.md
└── reference/
    └── drug-instructions/
```

## 推荐阅读

### 必读主线

| 顺序 | 文档 | 作用 |
| --- | --- | --- |
| 1 | [项目需求](requirements/project-requirements.md) | 明确项目目标、功能范围、医学安全边界和验收标准。 |
| 2 | [Agent 框架设计](architecture/agent-framework-design.md) | 明确当前架构、核心组件、接口扩展点和模块收敛方向。 |
| 3 | [开发规范](guides/development-guide.md) | 明确包结构、接口设计、SQLite + MyBatis Plus 存储层规范和文档规范。 |

### 按任务阅读

| 场景 | 继续阅读 |
| --- | --- |
| 修改 Agent 编排、工具选择、运行审计 | [Agent 并发与 SQLite 存储](architecture/agent-concurrency-sqlite-design.md) |
| 修改知识图谱读写 | [SQLite 知识图谱设计](architecture/sqlite-knowledge-graph-design.md) |
| 设计或实现 PDF 说明书入库 | [PDF 抽取与图谱设计](architecture/pdf-extraction-graph-design.md) |
| 使用代码结构索引和影响分析 | [CodeGraph 使用指南](guides/codegraph-guide.md) |

### 历史与复盘

以下文档保留历史决策背景，不作为当前实现真相源。若内容与必读主线冲突，以必读主线和当前代码为准。

| 文档 | 状态 |
| --- | --- |
| [架构优化计划](architecture/architecture-optimization-plan.md) | 历史复盘与阶段计划 |
| [聚焦交付复盘](architecture/focused-delivery-review.md) | 历史复盘 |
| [下一阶段任务](architecture/next-stage-tasks.md) | 历史任务拆解，需按当前代码复核 |
| [项目结构 Review](architecture/project-structure-review.md) | 历史结构评审 |
| [早期本地实现方案](guides/local-demo-implementation.md) | 历史参考，当前不作为正式开发规范 |

## 文档职责规则

| 文档类型 | 目录 | 写作要求 |
| --- | --- | --- |
| 需求、范围、验收标准 | `docs/requirements/` | 说明要解决什么问题，不展开实现细节。 |
| 架构设计、技术方案、演进计划 | `docs/architecture/` | 说明组件职责、边界、接口和关键决策。 |
| 开发规范、操作指南 | `docs/guides/` | 说明稳定规则和操作流程。 |
| 原始资料、样本文档 | `docs/reference/` | 保存外部资料，不混入设计说明。 |

新增稳定 Markdown 文档后，必须同步更新本文档。文档文件名使用小写英文和中划线，例如 `sqlite-knowledge-graph-design.md`。

## 当前技术决策

- 当前存储方案使用 SQLite。
- 当前存储层框架使用 MyBatis Plus。
- Agent 运行记录和知识图谱均应通过接口访问存储，业务层不直接操作 Mapper。
- 当前不是 demo 项目；功能可以简单，但命名、接口和文档应按正式开发组织。
- Agent 和 Agent 专属服务优先收敛在 `agent` 相关目录；只有跨上下文复用的能力才抽到通用 `service`、`knowledge`、`client` 或 `persistence`。

## 参考资料

药品说明书 PDF 等原始资料统一放在 [reference/drug-instructions](reference/drug-instructions/) 下。运行生成数据放入 `data/`，不要和文档混放。

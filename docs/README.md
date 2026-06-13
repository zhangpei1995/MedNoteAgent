# MedNoteAgent 文档中心

本目录是 MedNoteAgent 项目需求、最新架构设计、开发规范和参考资料的统一入口。文档职责应像代码职责一样清晰：入口文档只做索引，设计文档说明边界和决策，规范文档约束日常开发。

## 当前文档结构

```text
docs/
├── README.md
├── requirements/
│   └── project-requirements.md
├── architecture/
│   ├── agent-retrieval-optimization-design.md
│   └── business-module-structure-design.md
├── guides/
│   ├── development-guide.md
│   └── codegraph-guide.md
└── reference/
    └── drug-instructions/
```

## 推荐阅读

### 必读主线

| 顺序 | 文档 | 作用 |
| --- | --- | --- |
| 1 | [项目需求](requirements/project-requirements.md) | 明确项目目标、功能范围、医学安全边界和验收标准。 |
| 2 | [业务模块化目录结构设计](architecture/business-module-structure-design.md) | 明确按业务上下文组织代码、仅存储层横向独立的目标结构。 |
| 3 | [Agent 检索与推理优化设计](architecture/agent-retrieval-optimization-design.md) | 明确速度和效果均衡的分层检索、分级推理、模型和重排策略。 |
| 4 | [开发规范](guides/development-guide.md) | 明确包结构、接口设计、SQLite + MyBatis Plus 存储层规范和文档规范。 |

### 按任务阅读

| 场景 | 继续阅读 |
| --- | --- |
| 调整项目目录、包职责或业务模块边界 | [业务模块化目录结构设计](architecture/business-module-structure-design.md) |
| 修改 Agent、知识图谱、证据召回、PDF 入库或存储边界 | [业务模块化目录结构设计](architecture/business-module-structure-design.md) |
| 优化 Agent 耗时、query 规划、证据召回、rerank 或回答链路 | [Agent 检索与推理优化设计](architecture/agent-retrieval-optimization-design.md) |
| 使用代码结构索引和影响分析 | [CodeGraph 使用指南](guides/codegraph-guide.md) |

## 文档职责规则

| 文档类型 | 目录 | 写作要求 |
| --- | --- | --- |
| 需求、范围、验收标准 | `docs/requirements/` | 说明要解决什么问题，不展开实现细节。 |
| 架构设计 | `docs/architecture/` | 只保留当前最新设计，说明组件职责、边界、接口和关键决策。 |
| 开发规范、操作指南 | `docs/guides/` | 说明稳定规则和操作流程。 |
| 原始资料、样本文档 | `docs/reference/` | 保存外部资料，不混入设计说明。 |

新增稳定 Markdown 文档后，必须同步更新本文档。文档文件名使用小写英文和中划线，例如 `business-module-structure-design.md`。

## 当前技术决策

- 存储方案使用 SQLite。
- 存储层框架使用 MyBatis Plus。
- Agent 运行记录和知识图谱均应通过接口访问存储，业务层不直接操作 Mapper。
- 项目不是 demo；功能可以简单，但命名、接口和文档必须按正式开发组织。
- 代码按业务上下文收敛；除数据存储层独立管理外，Controller、DTO、应用服务、领域接口和业务实现优先放入所属业务模块。
- Agent 和 Agent 专属能力优先收敛在 `agent` 相关目录；知识图谱、证据片段和说明书入库能力优先收敛在 `knowledge`。
- Agent 问答外部入口只接收用户自然语言问题；任务主题、检索范围和风险信号由内部规划链路生成。
- Agent 证据召回采用分层检索和分级推理：FAST 优先，BALANCED 默认，高风险或低置信度时升级到 ACCURATE。
- Entity、Mapper、SQLite Store 实现和 schema 初始化集中在 `persistence`；业务层不直接操作 Mapper。

## 参考资料

药品说明书 PDF 等原始资料统一放在 [reference/drug-instructions](reference/drug-instructions/) 下。运行生成数据放入 `data/`，不要和文档混放。

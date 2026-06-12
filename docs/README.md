# MedNoteAgent 文档中心

本目录是 MedNoteAgent 项目文档、设计说明、开发指南和原始参考资料的统一入口。

## 目录结构

```text
docs/
├── README.md
├── requirements/
│   └── project-requirements.md
├── architecture/
│   ├── agent-framework-design.md
│   ├── architecture-optimization-plan.md
│   ├── focused-delivery-review.md
│   ├── next-stage-tasks.md
│   ├── pdf-extraction-graph-design.md
│   └── project-structure-review.md
├── guides/
│   ├── development-guide.md
│   └── local-demo-implementation.md
└── reference/
    └── drug-instructions/
        ├── 二冬汤颗粒（CXZS2500013）说明书.pdf
        └── 菖麻熄风颗粒（CXZS2500020）说明书.pdf
```

## 推荐阅读顺序

1. [Project Requirements](requirements/project-requirements.md)
2. [Agent Framework Design](architecture/agent-framework-design.md)
3. [Architecture Optimization Plan](architecture/architecture-optimization-plan.md)
4. [Project Structure Review](architecture/project-structure-review.md)
5. [Focused Delivery Review](architecture/focused-delivery-review.md)
6. [Next Stage Tasks](architecture/next-stage-tasks.md)
7. [PDF Extraction And Graph Design](architecture/pdf-extraction-graph-design.md)
8. [Development Guide](guides/development-guide.md)
9. [Local Demo Implementation](guides/local-demo-implementation.md)

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [项目需求](requirements/project-requirements.md) | 定义项目目标、功能范围、风险和验收标准。 |
| [Agent 框架设计](architecture/agent-framework-design.md) | 说明 Agent 编排模型、职责边界、扩展点和核心流程。 |
| [架构优化计划](architecture/architecture-optimization-plan.md) | 以批判视角列出当前问题、目标架构和分阶段优化计划。 |
| [项目结构 Review](architecture/project-structure-review.md) | 说明目录职责、精简边界和设计模式对应关系。 |
| [聚焦交付复盘](architecture/focused-delivery-review.md) | 复盘当前架构是否发散、职责是否清晰、演示和迭代如何持续。 |
| [下一阶段任务](architecture/next-stage-tasks.md) | P2/P3 任务拆解、验收标准和实施顺序。 |
| [PDF 抽取与图谱设计](architecture/pdf-extraction-graph-design.md) | 说明 PDF 解析、证据切片、结构化抽取、图谱节点和关系。 |
| [开发规范](guides/development-guide.md) | 定义包结构、编码规范、职责边界和开发约束。 |
| [本地 Demo 实现方案](guides/local-demo-implementation.md) | 提供本地 demo 的实现方案和最小可运行链路。 |

## 参考资料

药品说明书 PDF 等原始资料统一放在 [reference/drug-instructions](reference/drug-instructions/) 下。原始资料与设计文档分开管理，方便后续替换、版本化或批量导入。

## 命名规则

- 新增 Markdown 文档统一使用小写英文和中划线命名。
- 稳定项目文档放入对应分类目录，不放在仓库根目录。
- 原始资料放入 `docs/reference/`。
- 运行生成数据后续放入 `data/`，不要和文档混放。

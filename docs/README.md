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
│   └── pdf-extraction-graph-design.md
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
3. [PDF Extraction And Graph Design](architecture/pdf-extraction-graph-design.md)
4. [Development Guide](guides/development-guide.md)
5. [Local Demo Implementation](guides/local-demo-implementation.md)

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [项目需求](requirements/project-requirements.md) | 定义项目目标、功能范围、风险和验收标准。 |
| [Agent 框架设计](architecture/agent-framework-design.md) | 说明 Agent 编排模型、职责边界、扩展点和核心流程。 |
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

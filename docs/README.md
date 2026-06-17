# MedNoteAgent 文档中心

本目录只保留项目文档入口、真实目录结构和索引。Agent 执行规范以根目录 [AGENTS.md](../AGENTS.md) 为准，工程细节见 `guides/`。

## 目录结构

```text
docs/
├── README.md
├── guides/
│   ├── agent-handbook.md
│   └── development-conventions.md
├── requirements/
│   └── project-requirements.md
└── reference/
    └── drug-instructions/
        ├── 二冬汤颗粒（CXZS2500013）说明书.pdf
        └── 菖麻熄风颗粒（CXZS2500020）说明书.pdf
```

## 推荐阅读顺序

1. [AGENTS.md](../AGENTS.md)
2. [项目需求](requirements/project-requirements.md)
3. [Agent 协作手册](guides/agent-handbook.md)
4. [工程开发约定](guides/development-conventions.md)

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [项目需求](requirements/project-requirements.md) | 定义项目目标、功能范围、风险和验收标准。 |
| [Agent 协作手册](guides/agent-handbook.md) | 约束 Agent 如何检索项目、控制改动范围、保持模块职责和扩展边界。 |
| [工程开发约定](guides/development-conventions.md) | 约束技术栈、DAO、存储、删除清理和文档对齐规则。 |
| [药品说明书参考资料](reference/drug-instructions/) | 原始药品说明书 PDF。 |

## 维护规则

- 修改本文件的目录树、推荐阅读顺序或索引前，必须先用 `rg --files` 反查路径是否真实存在。
- 已删除或尚未创建的资料不要保留在索引中；确实是计划项时，应写入任务说明而不是伪装成现有文档。
- 新增稳定文档放入 `docs/requirements/`、`docs/guides/` 或明确的新分类目录；原始资料放入 `docs/reference/`。

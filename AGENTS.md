# MedNoteAgent Agent Instructions

本文件是 agent 进入本项目后的第一入口。执行代码开发、重构、评审、排障或设计落地前，必须先阅读本文件，并按索引继续读取相关文档。

## Required Reading

所有代码相关任务必须先阅读：

- `docs/guides/development-guide.md`
- `docs/guides/codegraph-guide.md`
- `docs/README.md`

如果任务涉及需求、架构、PDF 抽取、知识图谱、并发或存储，还必须按 `docs/README.md` 的索引读取对应文档。

## Documentation Index Rule

项目文档统一放在 `docs/` 下，并通过 `docs/README.md` 建立索引。

新增或导入文档时遵循：

- 需求和验收标准放入 `docs/requirements/`。
- 架构、模块设计和技术方案放入 `docs/architecture/`。该目录只保留当前最新设计。
- 编码规范、操作指南、开发流程放入 `docs/guides/`。
- 原始资料、外部资料和样本文档放入 `docs/reference/`。
- 新增稳定 Markdown 文档后，必须更新 `docs/README.md` 的目录结构、推荐阅读顺序或文档索引。
- 文档文件名使用小写英文和中划线，例如 `business-module-structure-design.md`。

## CodeGraph Requirement

CodeGraph 是本项目的代码智能索引和知识图谱工具，用于帮助 agent 快速理解代码结构、符号位置、调用关系和变更影响。

使用 CodeGraph 的目的：

- 在编码前建立全局上下文，避免只看局部文件就做设计或修改。
- 快速定位类、方法、接口和字段。
- 分析调用方、被调用方和变更影响范围。
- 判断修改后需要关注哪些测试或相关模块。

以下场景必须优先使用 CodeGraph：

- 开始代码开发、重构、排障或代码评审前。
- 修改已有类、方法、接口、包结构或核心流程前。
- 需要判断某个符号的调用关系、依赖关系或影响范围时。
- 需要根据变更文件推断受影响测试时。

CodeGraph 详细使用方式见：

- `docs/guides/codegraph-guide.md`

本项目 CodeGraph 索引位置：

- `.codegraph/codegraph.db`

该目录是本地索引目录，不提交到 Git。数据库只作为只读参考，不要手工修改。

常用命令：

```bash
codegraph status
codegraph init -i
codegraph sync
codegraph query <search>
codegraph callers <symbol>
codegraph callees <symbol>
codegraph impact <symbol>
codegraph affected <files...>
```

`codegraph init -i` 用于初始化索引，`codegraph sync` 用于同步上次索引后的变更。即使使用 CodeGraph，也必须结合 `rg`、源码阅读和测试验证。

## Development Rules

- 先用 `rg`、CodeGraph 和源码阅读理解现有结构，再改代码。
- 新增代码必须符合 `docs/guides/development-guide.md` 中的包职责、命名、业务模块分层和测试规范。
- 存储方案使用 SQLite，存储层框架使用 MyBatis Plus；业务层不得直接操作 Mapper。
- 项目按正式开发推进，不以 demo 作为代码或文档边界。
- 优先遵循现有架构、命名、包结构和测试风格。
- 不做无关重构。
- 不回滚用户已有改动。
- 涉及核心流程、共享接口、跨模块行为时，需要补充或更新测试。

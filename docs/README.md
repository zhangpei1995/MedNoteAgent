# MedNoteAgent 文档中心

本目录是 MedNoteAgent 项目文档、设计说明、开发指南和原始参考资料的统一入口。

## 目录结构

```text
docs/
├── README.md
├── requirements/
│   └── project-requirements.md
├── guides/
│   ├── agent-handbook.md
│   └── development-conventions.md
└── reference/
    └── drug-instructions/
        ├── 二冬汤颗粒（CXZS2500013）说明书.pdf
        └── 菖麻熄风颗粒（CXZS2500020）说明书.pdf
```

## 推荐阅读顺序

1. [Project Requirements](requirements/project-requirements.md)
2. [Agent Handbook](guides/agent-handbook.md)
3. [Development Conventions](guides/development-conventions.md)

## Agent 执行约定

后续 Agent 或开发者执行任务时，应先理解边界，再提交方案，最后实施和同步文档。根目录 [AGENTS.md](../AGENTS.md) 是最高频入口，[Agent 协作手册](guides/agent-handbook.md) 和 [工程开发约定](guides/development-conventions.md) 提供更完整的协作与工程规范。

### 标准流程

1. **先用 CodeGraph 建立上下文**
   - 使用 `codegraph query`、`codegraph callers`、`codegraph callees`、`codegraph impact`、`codegraph affected`、`codegraph files` 等命令了解相关符号、调用关系、影响范围和模块边界。
   - 如果项目还没有索引，使用 `codegraph init -i` 初始化生成。
   - 查看索引状态使用 `codegraph status`。
   - 查看更多命令说明使用 `codegraph --help` 或 `codegraph help <command>`。
2. **再用精确检索补充**
   - CodeGraph 收敛范围后，再用 `rg`、`rg --files` 查找关键词、配置项、测试或文档。
   - 避免无目的横向扫描项目。
3. **先生成计划方案**
   - 计划应说明目标、涉及模块、设计模式或扩展方式、改动文件、验证方式和风险。
   - 计划经过 Review 或用户确认后再执行。
4. **执行后保持一致**
   - 代码、配置、测试、文档需要同步对齐。
   - 完成后运行 `codegraph sync` 更新索引。
   - 如果同步前发现索引缺失或异常，先用 `codegraph status` 判断状态，再按 help 指引处理。

### 设计要求

- 可扩展逻辑优先使用接口、策略、工厂、适配器、模板方法或注册机制承载变化点。
- 当“同一件事情可以有多种实现”时，先抽象能力契约，再接入具体实现。
- 类、方法和参数要写清楚做什么、什么时候用、怎么用；必要时通过 JavaDoc 或注释说明入参、返回值、约束和扩展方式。
- 不把业务逻辑堆进 Controller、Agent 编排器或单个工具类；保持 `controller`、`agent`、`agent.tool`、`agent.runtime`、`service.spi`、`service.impl`、`domain`、`dto`、`config` 的职责分离。

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [项目需求](requirements/project-requirements.md) | 定义项目目标、功能范围、风险和验收标准。 |
| [Agent 协作手册](guides/agent-handbook.md) | 约束 Agent 如何检索项目、控制改动范围、保持模块职责和扩展边界。 |
| [工程开发约定](guides/development-conventions.md) | 约束 Spring Boot 3、SQLite、MyBatis Plus、`hutool-all`、删除清理和文档对齐规则。 |

## 参考资料

药品说明书 PDF 等原始资料统一放在 [reference/drug-instructions](reference/drug-instructions/) 下。原始资料与设计文档分开管理，方便后续替换、版本化或批量导入。

## 命名规则

- 新增 Markdown 文档统一使用小写英文和中划线命名。
- 稳定项目文档放入对应分类目录，不放在仓库根目录。
- 原始资料放入 `docs/reference/`。
- 运行生成数据后续放入 `data/`，不要和文档混放。

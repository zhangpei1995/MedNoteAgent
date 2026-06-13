# CodeGraph 使用指南

CodeGraph 是本项目的代码智能索引和知识图谱工具，用于快速理解代码结构、符号位置、调用关系和变更影响。

它的目标不是替代源码阅读，而是在编码前帮助 agent 和开发者先建立全局上下文，减少盲改、漏改和误判影响范围。

## 1. 使用目的

使用 CodeGraph 的主要目的：

1. 快速定位类、方法、接口、字段等符号。
2. 理解某个符号的调用方和被调用方。
3. 分析修改一个类或方法可能影响哪些代码。
4. 根据变更文件推断需要关注或运行的测试。
5. 为 agent 提供结构化代码上下文，辅助设计和编码决策。

## 2. 什么时候使用

以下场景必须优先使用 CodeGraph：

1. 开始一个代码开发任务前，先了解相关模块、类和方法。
2. 修改已有代码前，确认符号定义、调用方和影响范围。
3. 重构类、方法、包结构或接口前，分析依赖关系。
4. 修复缺陷前，定位问题链路和可能的调用路径。
5. 做代码评审前，理解变更涉及的上下游。
6. 新增测试或选择回归测试范围前，分析受影响文件。
7. agent 需要快速建立项目上下文时。

以下场景可以不使用 CodeGraph：

1. 只修改纯文档、注释或格式，且不涉及代码行为。
2. 只查看单个已知文件中的局部文本。
3. 执行与代码结构无关的简单命令。

即使使用 CodeGraph，也必须结合 `rg` 和源码阅读进行确认。

## 3. 索引位置

本项目的 CodeGraph 数据位于：

```text
.codegraph/codegraph.db
```

该目录是本地索引目录，不提交到 Git。数据库只作为只读参考，不要手工修改。

## 4. 初始化和同步

首次在项目中建立索引：

```bash
codegraph init -i
```

同步上次索引后的变更：

```bash
codegraph sync
```

如果不确定索引是否过期，先查看状态：

```bash
codegraph status
```

如果索引被锁文件阻塞：

```bash
codegraph unlock
```

## 5. 快速查询命令

查找符号：

```bash
codegraph query <search>
```

查看索引中的项目文件结构：

```bash
codegraph files
```

查找某个符号的调用方：

```bash
codegraph callers <symbol>
```

查找某个符号调用了哪些函数或方法：

```bash
codegraph callees <symbol>
```

分析修改某个符号的影响范围：

```bash
codegraph impact <symbol>
```

根据变更文件查找受影响测试：

```bash
codegraph affected <files...>
```

重新索引整个项目：

```bash
codegraph index
```

启动 CodeGraph MCP 服务，供 AI assistant 使用：

```bash
codegraph serve
```

安装到支持的 agent：

```bash
codegraph install
```

从支持的 agent 中移除：

```bash
codegraph uninstall
```

移除项目中的 CodeGraph 索引目录：

```bash
codegraph uninit
```

## 6. SQLite 兜底查询

当 `codegraph` CLI 不可用，或需要直接检查本地索引内容时，可以使用 SQLite 查询 `.codegraph/codegraph.db`。

查看表：

```bash
sqlite3 .codegraph/codegraph.db ".tables"
```

查找符号：

```bash
sqlite3 .codegraph/codegraph.db "
SELECT kind, name, qualified_name, file_path, start_line, end_line, signature
FROM nodes
WHERE lower(name) LIKE lower('%关键词%')
ORDER BY file_path, start_line;
"
```

查看文件内节点：

```bash
sqlite3 .codegraph/codegraph.db "
SELECT kind, name, qualified_name, start_line, end_line, signature
FROM nodes
WHERE file_path = 'src/...'
ORDER BY start_line;
"
```

查看调用或依赖关系：

```bash
sqlite3 .codegraph/codegraph.db "
SELECT e.kind, s.qualified_name AS source, t.qualified_name AS target, e.line
FROM edges e
JOIN nodes s ON e.source = s.id
JOIN nodes t ON e.target = t.id
WHERE s.qualified_name LIKE '%关键词%'
   OR t.qualified_name LIKE '%关键词%'
ORDER BY e.line;
"
```

## 7. Agent 使用要求

Agent 在本项目中工作时应遵循：

1. 开发前先阅读 `AGENTS.md`、`docs/README.md` 和任务相关文档。
2. 代码任务开始前，优先执行 `codegraph status` 判断索引状态。
3. 如果索引缺失，使用 `codegraph init -i` 初始化。
4. 如果代码已有变更，使用 `codegraph sync` 同步索引。
5. 使用 `codegraph query`、`callers`、`callees`、`impact` 等命令理解影响范围。
6. 最终仍以源码、测试和项目文档为准。

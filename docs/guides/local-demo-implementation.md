# MedNoteAgent 早期本地实现方案

> 状态：历史参考。当前项目按正式开发推进，不再以 demo 作为代码和文档边界。当前实现与后续开发以 [开发规范](development-guide.md)、[Agent 框架设计](../architecture/agent-framework-design.md) 和 [Agent 并发与 SQLite 存储设计](../architecture/agent-concurrency-sqlite-design.md) 为准。

## 当前结论

早期方案的价值在于验证了最小链路：

```text
说明书资料
  -> 章节识别
  -> 证据切片
  -> SQLite 存储
  -> 本地检索
  -> 风险判断
  -> 基于证据回答
```

这些方向仍然有效，但实现方式已经收敛为正式项目结构：

| 早期思路 | 当前要求 |
| --- | --- |
| 命令行本地演示 | Spring Boot API / SSE 接口 |
| 临时本地数据表 | SQLite 正式本地存储 |
| 手写 JDBC 或临时仓储 | MyBatis Plus Mapper + Store / Reader / Writer |
| demo 命名 | 正式业务命名 |
| 简单规则直接放流程里 | 通过接口隔离为可替换实现 |

## 保留原则

1. 先跑通证据链路，再扩展复杂模型和检索。
2. 当前功能可以轻量，但接口要稳定。
3. 回答必须带证据来源。
4. 证据不足时不能输出确定性医学结论。
5. 本地 SQLite 是当前正式存储基线，不再表述为临时演示存储。

## 不再作为规范的内容

以下早期设计不再作为当前开发依据：

- `demo` 包、`DemoApplication`、`DemoCommand` 等命名。
- 独立命令行 import / ask / stats 作为主入口。
- 不引入 Spring 的实现假设。
- 绕过 MyBatis Plus 的临时 SQLite 访问方式。
- 将当前功能称为 demo 或 mock 实现的文档口径。

如需实现新的本地功能，应先阅读 [开发规范](development-guide.md)，并按当前包结构、接口和存储层规范落地。

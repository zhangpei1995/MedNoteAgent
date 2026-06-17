# MedNoteAgent

MedNoteAgent 是一个基于 Spring Boot 3 的药品说明书知识管理与医学问答 Agent 项目。当前代码已收敛为本地可运行的 Agent 主链路：请求规划、说明书证据检索、风险评估和回答生成；完整的 PDF 入库、SQLite 持久化、向量检索和知识图谱仍属于后续建设内容。

## 快速启动

启动服务：

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' spring-boot:run
```

运行测试：

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' test
```

常用本地地址：

| 页面 | 地址 |
| --- | --- |
| Knife4j / Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 健康测试接口 | http://localhost:8080/api/test/ping |

## 当前接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/test/ping` | 返回服务状态、应用名称和当前时间 |
| POST | `/api/test/echo` | 验证 JSON 请求和参数校验 |

## 当前代码骨架

| 模块 | 当前状态 |
| --- | --- |
| `org.med.note.controller` | 暴露基础测试接口。 |
| `org.med.note.agent` | 编排规划、检索、风控和回答生成主链路，当前尚未暴露 HTTP Agent 接口。 |
| `org.med.note.service.spi` | 定义规划、证据检索、风险评估、回答生成四类可替换能力。 |
| `org.med.note.service.impl` | 提供本地实现，基于内置药品说明书片段生成可追溯回答。 |
| `org.med.note.client` | 封装千问客户端调用边界。 |
| `org.med.note.domain` / `org.med.note.dto` | 承载领域对象和接口数据结构。 |
| `org.med.note.config` | 承载 OpenAPI 与环境变量配置。 |

当前回归测试覆盖基础 Controller、SPI 契约和 Agent 主链路。说明书片段暂以内存数据承载在 `LocalDrugKnowledgeBase`，后续接入 SQLite 或检索引擎时应删除对应本地占位实现，而不是并行保留双路径。

千问调用默认关闭，避免本地测试依赖外部网络。需要启用真实模型时，配置 `mednote.llm.dashscope.enabled=true`，并通过环境变量 `QWEN_API_KEY` 提供密钥。

## 文档入口

所有项目文档统一维护在 [docs](docs/README.md) 目录下。

| 文档 | 用途 |
| --- | --- |
| [项目需求](docs/requirements/project-requirements.md) | 项目目标、功能范围、风险和验收标准。 |
| [Agent 协作手册](docs/guides/agent-handbook.md) | Agent 检索、职责边界、扩展方式和自检清单。 |
| [工程开发约定](docs/guides/development-conventions.md) | Spring Boot 3、SQLite、MyBatis Plus、`hutool-all`、删除清理和文档对齐规则。 |
| [药品说明书参考资料](docs/reference/drug-instructions/) | 原始药品说明书 PDF。 |

## Agent 工作约束

执行代码、配置或文档改动前，先阅读 [AGENTS.md](AGENTS.md)、[Agent 协作手册](docs/guides/agent-handbook.md) 和 [工程开发约定](docs/guides/development-conventions.md)。涉及 README、文档索引、目录树或链接清单时，必须先用真实文件列表反查，避免保留不存在的文件、接口或旧能力说明。

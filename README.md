# MedNoteAgent

MedNoteAgent 是一个基于 Spring Boot 的药品说明书知识管理与医学问答 Agent 项目。项目目标是将药品说明书转化为可检索、可追溯、可推理的证据知识，并提供本地 demo agent 接口用于在线调试。

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
| Scalar API 文档 | http://localhost:8080/docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 健康测试接口 | http://localhost:8080/api/test/ping |

## 文档入口

所有项目文档统一维护在 [docs](docs/README.md) 目录下。

| 分类 | 说明 |
| --- | --- |
| [项目需求](docs/requirements/project-requirements.md) | 项目目标、功能范围、风险和验收标准 |
| [Agent 框架设计](docs/architecture/agent-framework-design.md) | Agent 编排、模块边界和扩展点 |
| [PDF 抽取与图谱设计](docs/architecture/pdf-extraction-graph-design.md) | PDF 入库、证据片段、图谱节点和关系 |
| [开发规范](docs/guides/development-guide.md) | 代码组织、包职责、命名和实现规范 |
| [本地 Demo 方案](docs/guides/local-demo-implementation.md) | 本地 demo 范围、存储设计和实现步骤 |
| [参考资料](docs/reference/drug-instructions/) | 药品说明书原始 PDF |

## 当前 Demo 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/test/ping` | 基础健康测试 |
| POST | `/api/test/echo` | JSON 请求和参数校验测试 |
| POST | `/api/demo-agent/run` | 同步运行本地 demo agent |
| GET | `/api/demo-agent/stream` | 通过 SSE 流式运行本地 demo agent |

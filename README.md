# MedNoteAgent

MedNoteAgent 是一个准备重新实现的 Spring Boot 3 医学问答 Agent 项目。仓库当前保留工程基线、SQLite 初始化配置、药品说明书原始 PDF 和少量历史测试残留。

## 当前状态

| 项目 | 状态 |
| --- | --- |
| 应用入口 | `src/main/java/org/med/note/MedNoteAgentApplication.java` |
| 技术栈 | Spring Boot 3、Java 17、SQLite、MyBatis Plus、Knife4j、`hutool-all` |
| 配置 | `src/main/resources/application.yml` |
| SQLite schema | `src/main/resources/db/schema-sqlite.sql` |
| 原始资料 | `docs/reference/drug-instructions/` |
| 历史测试 | `src/test/java/` 下仍有旧测试残留，重新实现时需要按新功能清理或重建 |

## 快速启动

启动服务：

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' spring-boot:run
```

运行测试：

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' test
```

当前可预期的本地地址：

| 页面 | 地址 |
| --- | --- |
| Scalar API 文档 | http://localhost:8080/docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

> 当前 README 不声明任何业务接口可用。重新实现功能时，以新增代码、测试和文档同步后的状态为准。

## 文档入口

项目文档入口见 [docs/README.md](docs/README.md)。

当前文档只保留入口说明和原始参考资料：

| 文档 | 说明 |
| --- | --- |
| [参考资料](docs/reference/drug-instructions/) | 药品说明书原始 PDF |

## 重新实现原则

- 以当前目标重新设计功能。
- 新能力必须进入正式模块边界和正式调用链。
- 涉及医学问答时，必须保留证据引用、风险提示和可追溯信息。
- 修改 README、文档索引、目录树或链接清单前，必须用真实文件列表反向校验。

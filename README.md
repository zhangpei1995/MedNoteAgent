# MedNoteAgent

MedNoteAgent 是一个准备重新实现的 Spring Boot 3 医学问答 Agent 项目。仓库当前保留工程基线、SQLite 初始化配置、药品说明书原始 PDF 和少量历史测试残留。

## 当前状态

| 项目 | 状态 |
| --- | --- |
| 应用入口 | `src/main/java/org/med/note/MedNoteAgentApplication.java` |
| 技术栈 | Spring Boot 3、Java 17、SQLite、MyBatis Plus、Knife4j、`hutool-all` |
| 前端入口 | `frontend/` |
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

当前可预期的访问地址：

| 页面 | 地址 |
| --- | --- |
| Agent 对话前端 | http://localhost:5173 |
| Knife4j API 文档 | http://localhost:8080/doc.html |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

启动前端：

```bash
cd frontend
npm install
npm run dev
```

前端开发服务通过 Vite proxy 将 `/api` 转发到 `http://localhost:8080`，需要先启动后端服务。
如需代理到其他后端端口或域名，可在 `frontend/.env` 中配置：

```bash
VITE_API_PROXY_TARGET=http://localhost:8080
```

同一网络或服务器公网访问时，将 `localhost` 替换为实际主机 IP 或域名。

当前后端已提供会话提交、会话列表、会话轮次和轮次状态查询接口，前端对话页基于这些接口展示最小可用闭环。

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

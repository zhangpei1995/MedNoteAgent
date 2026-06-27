# MedNoteAgent

MedNoteAgent 是一个基于 Spring Boot 3 的医学问答 Agent 项目。当前阶段已经完成基础聊天闭环：前端提交用户问题，后端创建或复用会话，写入轮次审计记录，事务提交后异步调用 DashScope 大模型，并把执行结果回写到 SQLite。

当前实现重点是把正式调用链跑通，而不是构建临时 demo。后续近期工作围绕测试、医学证据追溯、异常状态和参考资料结构化继续收敛。

## 当前功能

| 能力 | 当前实现 |
| --- | --- |
| 后端应用 | Spring Boot 3 应用入口：`src/main/java/org/med/note/MedNoteAgentApplication.java` |
| 会话接口 | `POST /api/chat/sessions` 提交一轮问题；`GET /api/chat/sessions` 查询会话列表；`GET /api/chat/sessions/{sessionId}/turns` 查询会话轮次；`GET /api/chat/turns/{turnId}` 查询轮次状态 |
| 会话服务 | `ChatSessionService` 负责对外会话契约，`ChatTurnSubmissionRuntime` 负责编排会话创建、轮次审计和异步 Agent 执行 |
| Agent 执行 | `DashscopeChatAgentExecutor` 读取 `src/main/java/org/med/note/agent/AgentPrompt.md`，调用 DashScope 模型生成回答 |
| 数据存储 | SQLite 数据库默认写入 `data/mednote-agent.sqlite`；schema 位于 `src/main/resources/db/schema-sqlite.sql` |
| 审计记录 | `chat_turn_audit` 保存用户输入、模型输出、模型信息、系统提示词、请求响应 JSON、状态、错误信息和耗时 |
| 前端工作台 | `frontend/` 提供 Vite + React 对话页，支持会话列表、会话切换、消息提交和轮次状态刷新 |
| API 文档 | 集成 Springdoc 和 Knife4j，可查看后端接口 |
| 参考资料 | `docs/reference/drug-instructions/` 存放当前原始药品说明书 PDF |

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

## 当前闭环

当前聊天链路如下：

```text
前端对话页
  -> POST /api/chat/sessions
  -> ChatSessionService
  -> ChatTurnSubmissionRuntime
  -> chat_session / chat_turn_audit
  -> 事务提交后异步执行 Agent
  -> DashScope 模型调用
  -> 回写 chat_turn_audit
  -> 前端查询轮次状态并展示回答
```

这条链路已经进入正式模块边界：

- Controller 只负责 HTTP 入参出参。
- Service 暴露会话能力契约。
- Runtime 编排一轮提交和异步执行。
- Executor 负责模型调用。
- DAO 和实体负责 SQLite 持久化。
- 前端通过统一 API 模块访问后端接口。

## 文档入口

项目文档入口见 [docs/README.md](docs/README.md)。

当前文档保留入口说明、原始参考资料和近期文档工作说明：

| 文档 | 说明 |
| --- | --- |
| [参考资料](docs/reference/drug-instructions/) | 药品说明书原始 PDF |

## 近期工作

近期工作只围绕当前基础闭环继续补齐，不展开过大的长期规划：

1. 补齐测试
   - 覆盖会话创建、追加轮次、会话列表、轮次状态查询。
   - 覆盖 Agent 成功、失败和异常回写。
   - 补充前端 API 调用和关键交互的基础验证。

2. 完善医学安全和追溯
   - 明确回答中证据引用、风险提示和不确定性提示的输出格式。
   - 评估 `chat_turn_audit` 是否需要增加结构化证据字段。
   - 保留原始请求、原始响应、系统提示词和错误信息，方便审计。

3. 整理参考资料处理流程
   - 先围绕现有两份药品说明书设计最小结构化方案。
   - 明确资料抽取、人工校验、证据片段保存和回答引用方式。
   - 不提前创建空文档或空目录。

4. 改进状态和错误体验
   - 后端统一轮次状态语义。
   - 前端区分等待中、执行中、已完成和失败状态。
   - 对模型调用失败、会话不存在、输入为空等场景给出清晰反馈。

5. 保持文档与代码一致
   - 新增或删除入口、目录、接口、配置时同步更新 README。
   - 修改文档索引、目录树或链接清单前，用真实文件列表反向校验。

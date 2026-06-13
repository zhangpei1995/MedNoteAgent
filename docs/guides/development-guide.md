# MedNoteAgent 开发规范说明

本文档是 MedNoteAgent 的整体编写规范，约束代码组织、接口设计、存储层、测试和文档职责。当前项目按正式开发推进，不以 demo 作为模块边界；功能可以先简单，但命名、接口和扩展点必须面向长期演进。

## 1. 总体原则

1. 设计先收敛，再扩展。新增组件前先判断是否属于现有 Agent、知识、存储或客户端边界。
2. 核心组件先用好，不为未来能力提前堆目录、模式或工具。
3. 业务能力通过接口隔离实现，方便替换模型、检索、风险策略、解析器和存储实现。
4. 当前存储方案使用 SQLite，存储层框架使用 MyBatis Plus。
5. 业务层不得直接操作 MyBatis Mapper；应通过 Store、Repository 或明确的领域读写接口访问数据。
6. Agent 和 Agent 专属服务优先收敛到 `agent` 相关目录；跨上下文复用的能力再抽到通用模块。
7. 不写“demo 代码”。临时或轻量实现也应使用正式命名，例如 `RuleBasedRequestPlanner`，不使用 `DemoRequestPlanner`、`MockXxx` 作为长期类名。
8. 修改代码前应结合 `rg`、CodeGraph 和源码阅读理解现有实现。CodeGraph 细节见 [CodeGraph 使用指南](codegraph-guide.md)。
9. 涉及核心流程、共享接口、存储结构或跨模块行为时，必须补充或更新测试。

## 2. 推荐包结构

项目采用 Maven 标准目录结构，Java 源码统一放在 `src/main/java/org/med/note` 下。

```text
src/main/java/org/med/note
├── MedNoteAgentApplication.java
├── agent/                 # Agent 聚焦上下文：编排、工具、运行时、Agent 专属能力
│   ├── runtime/           # 调度、并发、上下文合并、运行审计
│   └── tool/              # Agent 工具协议和工具入口
├── client/                # 外部服务客户端，如模型、OCR、文件服务
├── config/                # Spring、OpenAPI、SQLite schema、配置装配
├── controller/            # HTTP/SSE 协议入口，只做参数转换和响应封装
├── dao/                   # MyBatis Plus Mapper
├── domain/                # 领域模型和值对象
├── dto/                   # API 入参、出参和事件传输对象
├── knowledge/             # 知识库和知识图谱领域能力
├── persistence/entity/    # MyBatis Plus 实体
├── service/               # 跨上下文复用的业务接口和实现
└── util/                  # 无业务含义的纯工具
```

收敛规则：

- Agent 编排、工具选择、工具上下文、工具执行、运行审计属于 `agent`。
- 如果 planning、retrieval、safety、generation 只服务于 Agent，优先放入 `agent` 的子包或 Agent 专属接口。
- 如果某能力会被入库、批处理、API 或其他上下文复用，再放入通用 `service` 或领域模块。
- `dao` 和 `persistence/entity` 只表达持久化结构，不承载业务流程。

测试代码放在 `src/test/java/org/med/note` 下，并保持与主代码一致的包路径。

## 3. Agent 设计规范

Agent 负责组织任务流程，不直接写医学规则、模型 SDK 调用、PDF 解析或底层 SQL。

当前核心组件边界：

| 组件 | 职责 |
| --- | --- |
| `MedNoteAgent` | 请求级编排、循环推进、结果保存。 |
| `AgentToolPlanner` | 根据上下文、依赖和工具元数据选择下一批工具。 |
| `AgentToolExecutor` | 执行工具并生成标准调用记录。 |
| `AgentContextMerger` | 合并工具输出，避免工具直接修改共享上下文。 |
| `AgentRunStore` | 保存和查询运行审计记录。 |
| `AgentTool` | 可替换能力入口，不承载大量业务细节。 |

工具粒度以“可替换能力”为边界，不以私有函数为边界。当前建议保留四类核心能力：

```text
request_planning
drug_knowledge_search
medical_risk_assessment
answer_generation
```

新增工具前必须说明：为什么不能放入已有工具或已有能力接口中。

## 4. 接口和实现规范

1. 先定义稳定接口，再提供当前轻量实现。
2. 接口命名表达能力，例如 `RequestPlanner`、`EvidenceRetriever`、`RiskAssessor`、`AnswerGenerator`。
3. 实现命名表达策略或数据来源，例如 `RuleBasedRequestPlanner`、`SqliteEvidenceRetriever`、`QianwenAnswerGenerator`。
4. 避免使用 `Demo`、`Mock`、`Temp`、`TestOnly` 作为主代码类名。测试替身只能放在测试包。
5. 当前实现可以规则化、模板化或使用本地数据，但接口参数和返回值要为后续真实实现留出必要字段。

## 5. 存储层规范

当前存储方案：

```text
SQLite
  -> MyBatis Plus Mapper
  -> persistence/entity
  -> Store / Repository / Reader / Writer 接口
  -> Agent / Knowledge / Service
```

要求：

- 使用 `persistence/entity` 表达数据库表结构，实体类使用 MyBatis Plus 注解。
- 使用 `dao` 放置 `BaseMapper<XxxEntity>`。
- 使用 Store、Repository、Reader、Writer 封装 Mapper 调用。
- 业务层不直接依赖 Mapper，不拼接 SQL。
- JSON 快照字段只用于审计、调试和兼容演进；高频查询字段应拆为标准列。
- SQLite 是当前正式本地存储基线，不写成 demo 存储。后续迁移 MySQL 时应保持上层接口不变。

表结构或实体变更时，需要同步检查：

- `SqliteSchemaInitializer`
- MyBatis Plus entity 和 mapper
- 对应 Store / Repository 测试
- 相关架构详情文档

## 6. Handler 使用规范

Handler 用于聚焦但复杂的单次流程，例如 PDF 入库、医学问答、图谱写入。它可以共享本次处理上下文，但不得保存跨请求可变状态。

使用要求：

- 一个 Handler 只处理一个明确任务。
- Handler 的公共入口应少而清晰，例如 `handle` 或 `execute`.
- Handler 可以调用 service、repository、client、strategy，但不绕过存储接口直接操作 Mapper。
- 如果作为 Spring 单例 Bean，不使用实例字段保存请求状态；优先使用方法内上下文对象。
- 如果 Handler 继续膨胀，应拆出策略、客户端、仓储或子流程对象。

## 7. Controller、Client 和 DTO 规范

Controller：

- 只做协议转换、参数校验、响应封装和 SSE 输出。
- 不直接调用工具、模型客户端或 Mapper。
- 不暴露内部工具参数作为外部稳定 API。

Client：

- 每个客户端只封装一个外部系统或 SDK。
- API Key、模型名、地址、超时时间必须配置化。
- 外部异常在 client 层转换为项目异常或明确的失败结果。

DTO：

- DTO 只承载接口传输数据和必要校验。
- 请求对象使用 `XxxRequest`，响应对象使用 `XxxResponse`，事件对象使用 `XxxEvent` 或明确业务名。

## 8. 命名规范

- Java 类名使用大驼峰，包名小写。
- 配置文件和 Markdown 文件使用小写英文和中划线。
- 服务接口以能力命名，避免 `Manager`、`Processor` 这类泛化名称。
- 复杂流程类可使用 `Handler`，但名称必须体现业务目标。
- 当前正式代码不使用 demo 命名。历史接口路径或类名如暂未迁移，应在文档中标注为历史命名。

## 9. 文档规范

文档职责应清晰：

- `README.md`：项目入口、启动方式、当前技术基线。
- `docs/README.md`：文档索引和阅读路径。
- `requirements/`：需求和验收，不展开实现细节。
- `architecture/`：架构边界、核心组件、接口和关键决策。
- `guides/`：稳定开发规范和操作指南。
- 详情文档可以写表结构、流程和边界条件；普通设计文档不应过细。

Markdown 以 IntelliJ IDEA / JetBrains 默认预览为主要阅读环境：

- 优先使用 `text` 代码块、Markdown 表格、缩进树和编号步骤。
- 不依赖 Mermaid、PlantUML 等额外渲染器。
- 如果保留图语法，必须同时提供文本版结构。

新增或稳定修改文档时，应同步更新 [文档中心](../README.md)。

## 10. 测试规范

1. Agent 编排、工具选择、存储读写、知识图谱、风险判断和回答生成都应有测试覆盖。
2. 外部模型调用通过 Mock、测试替身或可控 fixture 隔离。
3. 存储层测试应覆盖写入、读取、重复写入和失败边界。
4. 修改接口或表结构时，应补充能证明兼容性的测试。
5. 评测用例用于检查工具链、证据、风险等级和回答质量是否退化。

## 11. 提交流程检查清单

- 是否保持模块收敛，没有新增无必要目录或工具。
- Agent 是否只负责编排，业务细节是否通过接口隔离。
- 存储访问是否经过 Store / Repository / Reader / Writer。
- 是否遵循 SQLite + MyBatis Plus 当前基线。
- 是否避免 demo/mock/temp 命名进入主代码。
- 是否更新了相关文档索引和详情文档。
- 是否补充或更新测试。
- 是否没有提交密钥、本地数据库、IDE 配置和无关改动。

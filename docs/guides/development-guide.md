# MedNoteAgent 开发规范说明

本文档用于约束 MedNoteAgent 项目的代码组织、命名、注释、设计模式和职责划分。所有新增代码、重构代码和评审代码都应遵循本规范。

## 1. 总体原则

1. 代码应优先保证清晰、可维护、可测试，再考虑技巧性写法。
2. 每个类、方法、包都应有明确职责，避免把业务流程、外部接口调用、数据转换和配置读取混在同一处。
3. 业务逻辑应通过接口和实现解耦，方便后续替换模型服务、数据来源、解析策略或输出格式。
4. 对外部系统的访问必须集中封装，不能在业务类中散落 HTTP、SDK、API Key、重试等细节。
5. 配置、密钥、路径、模型名称等可变信息不得硬编码在业务逻辑中，应放入配置文件、环境变量或配置中心。
6. 新增能力应配套必要的单元测试或集成测试，至少覆盖核心流程、异常流程和边界输入。
7. 当一个功能聚焦但流程复杂、需要共享较多中间状态时，应使用 `handler` 承载该功能的完整处理过程，避免在 `service` 中堆积过多业务细节，也避免多个方法之间反复传递大量参数。

## 2. 推荐目录结构

项目采用 Maven 标准目录结构，Java 源码统一放在 `src/main/java/org/med/note` 下。

```text
src/main/java/org/med/note
├── MedNoteAgentApplication.java       # 应用启动入口
├── agent/                             # Agent 编排层，负责任务流程调度
├── client/                            # 外部服务客户端，如大模型、OCR、文件服务
├── config/                            # 配置读取、Bean 装配、常量配置
├── controller/                        # Web/API 入口，如后续接入 Spring MVC
├── domain/                            # 领域模型和值对象
├── dto/                               # 入参、出参、接口传输对象
├── exception/                         # 自定义异常和统一异常处理
├── handler/                           # 聚焦复杂功能的处理器，封装流程状态和执行细节
├── repository/                        # 数据访问、文件访问、向量库访问
├── service/                           # 业务服务接口和实现
├── strategy/                          # 可替换策略，如模型选择、摘要生成、解析策略
└── util/                              # 通用工具类，只放无业务含义的纯工具方法
```

测试代码统一放在 `src/test/java/org/med/note` 下，并保持与主代码一致的包路径。

```text
src/test/java/org/med/note
├── agent/
├── client/
├── handler/
├── service/
└── strategy/
```

## 3. 包职责规范

### 3.1 `agent`

`agent` 包用于编排完整任务流程，例如医学说明书读取、信息抽取、内容总结、结果输出。Agent 可以调用 `service`，但不应直接调用外部 SDK 或拼装复杂请求参数。

适合放置：

- `MedNoteAgent`
- `InstructionAnalysisAgent`
- `ClinicalSummaryAgent`

不适合放置：

- HTTP 客户端实现
- API Key 读取逻辑
- 通用字符串工具方法
- 数据库或文件系统底层操作

### 3.2 `client`

`client` 包用于封装外部服务调用。每个客户端只负责一个外部系统或一种 SDK，不承载业务规则。

示例：

- `QianwenClient` 只负责千问模型调用、请求参数构建、响应解析和外部异常转换。
- 后续如果接入 OCR，应创建 `OcrClient` 或更具体的实现类。

### 3.3 `service`

`service` 包用于表达对外业务能力，应优先定义接口，再提供实现类。`service` 不应承载过多细粒度业务逻辑；当某个功能流程聚焦但步骤较多、状态较多时，应委托给 `handler` 完成。

示例：

```text
service/
├── MedicalNoteService.java
└── impl/
    └── MedicalNoteServiceImpl.java
```

接口负责表达业务能力，实现类负责组织入口参数、选择合适处理器、调用仓储或客户端、处理事务边界和业务异常。

`service` 中不推荐出现：

- 大段多步骤业务流程。
- 多个私有方法共享十几个参数。
- 同时处理解析、校验、检索、组装、写入等细节。
- 为了串联流程而创建大量临时变量并层层传递。

### 3.4 `handler`

`handler` 包用于承载“聚焦但复杂”的功能处理流程。Handler 可以持有该功能执行过程中需要共享的类属性或上下文字段，减少方法之间反复传参，让代码围绕一个明确任务展开。

适合使用 Handler 的场景：

- PDF 说明书入库：解析、清洗、章节识别、切片、抽取、写入、报告生成。
- Agent 问答执行：意图识别、检索、证据聚合、风险评估、回答生成、回答校验。
- 知识图谱写入：实体归一、节点合并、关系创建、证据绑定。
- 一次处理过程有多个步骤，且多个步骤共享同一批中间结果。

Handler 设计要求：

- 一个 Handler 只处理一个聚焦功能，例如 `PdfInstructionIngestionHandler`、`MedicalQuestionAnswerHandler`。
- Handler 可以使用类属性保存本次处理共享的上下文，但不得保存跨请求的可变全局状态。
- Handler 的公共入口方法应尽量少，通常为 `handle`、`execute` 或语义更明确的方法。
- Handler 内部方法围绕步骤拆分，方法之间优先共享上下文对象或实例字段，避免长参数列表。
- Handler 可以调用 `service`、`repository`、`client`、`strategy`，但不能绕过职责边界直接做底层存储细节。
- 如果项目引入 Spring，Handler 默认使用原型作用域或方法内创建上下文，避免单例 Bean 中保存请求级状态。

示例：

```text
handler/
├── PdfInstructionIngestionHandler.java
├── MedicalQuestionAnswerHandler.java
└── KnowledgeGraphWriteHandler.java
```

推荐结构：

```java
/**
 * PDF 说明书入库处理器。
 *
 * <p>封装单次入库过程中的解析、清洗、切片、抽取和写入流程。
 * 该类只保存本次处理的上下文，不保存跨请求状态。</p>
 */
public class PdfInstructionIngestionHandler {

    private IngestionContext context;

    public IngestionReport handle(IngestionRequest request) {
        this.context = IngestionContext.from(request);
        parseDocument();
        recognizeSections();
        buildEvidenceChunks();
        extractEntities();
        writeKnowledge();
        return context.toReport();
    }

    private void parseDocument() {
        // 当前步骤可以直接读取 context，避免长参数列表。
    }
}
```

### 3.5 `domain`

`domain` 包用于放置领域模型和值对象，字段命名应体现业务含义。

示例：

- `MedicalInstruction`
- `DrugInfo`
- `ExtractionResult`
- `PromptTemplate`

### 3.6 `dto`

`dto` 包用于接口传输对象。DTO 不应包含复杂业务逻辑，只用于数据承载和必要的参数校验。

命名建议：

- 请求对象：`XxxRequest`
- 响应对象：`XxxResponse`
- 内部命令对象：`XxxCommand`

### 3.7 `strategy`

当同一业务点存在多种可替换实现时，应使用策略模式放入 `strategy` 包。

适用场景：

- 不同模型的选择策略
- 不同文档类型的解析策略
- 不同摘要风格的生成策略
- 不同输出格式的渲染策略

### 3.8 `util`

`util` 包只允许放置无业务含义、可复用的纯工具方法。例如日期格式转换、集合判空、文件大小格式化等。带业务含义的方法应放入对应的 `service`、`domain` 或 `strategy`。

## 4. 类职责规范

1. 一个类只负责一个明确主题，避免出现万能类、超长类和过多静态方法。
2. 一个类如果同时负责流程编排、外部调用、数据转换和异常处理，应拆分为多个类。
3. 类名必须能直接表达职责，不使用 `Manager`、`Processor` 等泛化名称。`Handler` 只用于聚焦复杂功能的处理器，命名必须体现具体业务目标，例如 `PdfInstructionIngestionHandler`，不能命名为 `CommonHandler` 或 `DataHandler`。
4. 外部客户端类以 `Client` 结尾，例如 `QianwenClient`。
5. 业务服务接口以 `Service` 结尾，实现类以 `ServiceImpl` 结尾。
6. 策略接口以 `Strategy` 结尾，具体实现体现策略名称，例如 `QianwenModelSelectStrategy`。
7. 工厂类以 `Factory` 结尾，只负责创建对象，不承载业务流程。
8. 配置类以 `Config` 或 `Properties` 结尾。
9. 复杂功能处理器以 `Handler` 结尾，且应有清晰的处理边界和生命周期。

## 5. 文件命名规范

1. Java 类文件名必须与公开类名完全一致，使用大驼峰命名法。
2. 包名全部小写，使用业务含义明确的单词，不使用拼音和缩写。
3. 常量名使用全大写加下划线，例如 `DEFAULT_MODEL`。
4. 方法名、变量名使用小驼峰命名法，例如 `parseResult`、`messageList`。
5. 测试类以被测类名加 `Test` 结尾，例如 `QianwenClientTest`。
6. 配置文件使用小写加中划线，例如 `application-local.yml`。
7. 文档文件统一使用小写英文和中划线，保持可读，例如 `development-guide.md`、`api-design.md`。

## 5.1 Markdown 文档规范

项目 Markdown 文档以 IntelliJ IDEA / JetBrains 系列 IDE 的默认 Markdown 预览为主要阅读环境。

要求：

- 图结构优先使用 `text` 代码块、Markdown 表格、缩进树和列表表达。
- 不依赖 Mermaid、PlantUML 等需要额外插件或渲染器的图语法。
- 如果确实需要保留 Mermaid 或 PlantUML，必须同时提供一份 IDEA 默认可读的文本版结构。
- 关系图应优先使用“起点节点 / 关系 / 终点节点 / 说明”的表格表达。
- 流程图应优先使用缩进树或编号步骤表达。

## 6. 注释规范

代码需要写注释，但注释应解释意图、约束和原因，避免重复描述代码本身。

### 6.1 类注释

所有对外可见的核心类都应编写类注释，说明职责、使用场景和边界。

```java
/**
 * 千问模型客户端。
 *
 * <p>负责封装 DashScope SDK 的请求构建、调用和响应解析。
 * 不承载医学业务规则，业务编排应放在 service 或 agent 层。</p>
 */
public class QianwenClient {
}
```

### 6.2 方法注释

公共方法、复杂私有方法、存在重要约束的方法应编写方法注释。

```java
/**
 * 使用指定系统提示词和用户问题发起单轮对话。
 *
 * @param systemPrompt 系统角色提示词，可以为空
 * @param userContent 用户输入内容，不能为空
 * @param model 模型名称，不能为空
 * @return 模型返回的文本内容；当服务无有效输出时返回空字符串
 */
public String chat(String systemPrompt, String userContent, String model) {
    return "";
}
```

### 6.3 行内注释

行内注释只用于解释不明显的业务规则、兼容逻辑、异常兜底或第三方 SDK 限制。

推荐：

```java
// DashScope 要求 system message 放在首位，否则会影响角色设定。
messageList.add(systemMessage);
```

不推荐：

```java
// 创建 List
List<Message> messageList = new ArrayList<>();
```

### 6.4 TODO 注释

临时代码必须使用统一格式，并说明后续处理人或处理方向。

```java
// TODO: 将 API Key 改为从环境变量读取，避免密钥进入仓库。
```

## 7. 设计模式使用规范

设计模式用于降低复杂度，不应为了模式而模式。只有当变化点明确、重复逻辑明显或对象创建复杂时才引入。

### 7.1 单例模式

适用于无状态、线程安全、创建成本高的客户端或配置对象。

要求：

- 单例对象不得持有请求级可变状态。
- 初始化失败要有明确异常。
- 如项目引入 Spring，应优先交给容器管理。

### 7.2 策略模式

适用于同一业务行为存在多种实现，且需要在运行时选择。

示例：

```text
strategy/
├── PromptBuildStrategy.java
├── DrugInstructionPromptBuildStrategy.java
└── ClinicalSummaryPromptBuildStrategy.java
```

### 7.3 工厂模式

适用于对象创建逻辑复杂，或需要根据类型创建不同实现。

示例：

```text
factory/
└── PromptStrategyFactory.java
```

工厂只负责选择和创建对象，不负责执行业务流程。

### 7.4 模板方法模式

适用于多个流程步骤基本一致，但其中部分步骤可被子类定制的场景。

示例：

- 文档读取
- 内容清洗
- 模型调用
- 结果解析

### 7.5 适配器模式

适用于屏蔽第三方 SDK 或不同模型服务的接口差异。

示例：

```text
client/
├── ModelClient.java
├── QianwenModelClientAdapter.java
└── OpenAiModelClientAdapter.java
```

业务层只依赖 `ModelClient` 接口，不直接依赖具体 SDK。

### 7.6 Handler 模式

适用于功能目标明确、流程步骤较多、多个步骤共享同一批中间状态的场景。

示例：

```text
handler/
├── PdfInstructionIngestionHandler.java
├── MedicalQuestionAnswerHandler.java
└── KnowledgeGraphWriteHandler.java
```

使用要求：

- Service 只负责入口、事务、权限、异常边界和处理器选择。
- Handler 负责单次复杂功能的执行过程和上下文状态。
- Handler 内部可以按步骤拆分私有方法，优先通过上下文对象或实例字段共享状态。
- Handler 不得保存跨请求可变状态，避免并发串数据。
- 如果 Handler 逻辑继续膨胀，应再拆出 `strategy`、`client`、`repository` 或子 Handler。

## 8. 异常处理规范

1. 不允许吞掉异常后只返回空值，除非调用方明确接受空结果并有注释说明。
2. 外部 SDK 异常应在 `client` 层转换为项目自定义异常。
3. 业务校验失败使用业务异常，例如 `BusinessException`。
4. 参数为空、格式错误等问题应尽早校验，错误信息要能帮助定位问题。
5. 捕获异常时应保留原始异常，避免丢失堆栈。

示例：

```java
throw new ModelCallException("调用千问模型失败", e);
```

## 9. 配置和密钥规范

1. API Key、接口地址、模型名称、超时时间等必须配置化。
2. 本地开发配置不得提交真实密钥。
3. 推荐使用环境变量，例如 `DASHSCOPE_API_KEY`。
4. 配置类只负责读取和校验配置，不承载业务逻辑。
5. 文档中可以给出示例值，但不能写真实可用密钥。

## 10. 日志规范

1. 不使用 `System.out.println` 输出业务日志，统一使用日志框架。
2. 日志应包含关键上下文，例如任务 ID、文档名、模型名、耗时。
3. 不记录完整密钥、用户隐私、敏感医疗信息。
4. 异常日志应保留堆栈。
5. 高频循环中避免打印大量日志。

## 11. 测试规范

1. 核心业务服务必须有单元测试。
2. 外部客户端调用应通过 Mock 或测试替身隔离，不依赖真实远程服务。
3. 解析、提示词构建、策略选择等逻辑应覆盖正常输入、空输入、异常输入。
4. 测试方法名应描述场景和期望结果。
5. 修复缺陷时应补充能复现问题的测试。

测试命名示例：

```java
shouldReturnEmptyTextWhenModelHasNoChoice()
shouldThrowExceptionWhenMessageListIsEmpty()
```

## 12. 提交流程检查清单

提交代码前至少检查以下事项：

- 类名、方法名、包名是否符合命名规范。
- 类职责是否单一，是否存在过长方法或万能类。
- 对外方法是否有必要的 JavaDoc。
- 复杂逻辑是否有解释意图的注释。
- 外部服务调用是否封装在 `client` 层。
- 聚焦但复杂的功能是否已使用 `handler` 承载，而不是堆在 `service` 中。
- Handler 是否只保存单次处理状态，没有跨请求可变状态。
- 可变配置和密钥是否已从代码中移除。
- 异常是否有明确处理和上下文信息。
- 新增逻辑是否有测试覆盖。
- 是否没有提交临时文件、真实密钥、IDE 本地配置和无关改动。

## 13. 推荐落地步骤

1. 先整理基础包结构：`config`、`domain`、`dto`、`exception`、`handler`、`service`、`strategy`。
2. 将外部模型调用抽象为接口，例如 `ModelClient`，再由 `QianwenClient` 实现或适配。
3. 将 Agent 中的流程编排和具体业务能力拆开，Agent 只负责任务调度。
4. 对 PDF 入库、医学问答、图谱写入等复杂聚焦功能创建对应 Handler，避免在 Service 中写过长流程。
5. 将 API Key、模型名、接口地址迁移到配置或环境变量。
6. 为客户端解析逻辑、策略选择逻辑、Handler 主流程和核心服务补充测试。

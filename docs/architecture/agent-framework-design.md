# MedNoteAgent Agent 框架设计文档

## 1. 背景与目标

MedNoteAgent 后续将建设药物说明书知识库，面向用户问题给出基于药品说明书证据的回答，例如：

- 药品禁忌
- 用药建议
- 药理作用
- 相互作用和药理冲突
- 特殊人群用药提醒
- 不良反应和注意事项

本设计文档先定义 Agent 框架的职责边界、模块划分、扩展点和核心流程，后续编码应以此为蓝图逐步落地。

## Markdown 阅读环境说明

本项目 Markdown 文档以 IntelliJ IDEA / JetBrains 系列 IDE 的默认 Markdown 预览效果为主要适配目标。

编写图结构时遵循以下约定：

- 优先使用 `text` 代码块、Markdown 表格和缩进树表达结构。
- 不依赖 Mermaid、PlantUML 等需要额外插件或渲染器的图语法。
- 如果必须保留图语法，应同时提供一份 IDEA 默认可读的文本版结构。
- 关系图、流程图、节点图应保证在 IDEA 中即使不渲染为图片，也能直接读懂。

## 2. 设计原则

1. **证据优先**：回答必须优先基于药品说明书、结构化知识库和可追溯片段，不允许凭空编造。
2. **职责清晰**：Agent 只负责任务编排，不直接处理 PDF 解析、向量检索、模型 SDK 调用等底层细节。
3. **可扩展**：模型、检索引擎、文档解析器、回答策略、风险判断策略都应可以替换。
4. **安全保守**：涉及禁忌、孕妇儿童、合并用药、严重不良反应时，应触发安全策略，输出谨慎提示。
5. **可追溯**：回答应包含依据来源，例如药品名称、说明书章节、原文片段或知识条目 ID。
6. **可测试**：核心流程应拆成独立接口，便于 Mock 外部模型和检索服务。
7. **Handler 承载复杂聚焦流程**：当一个功能目标明确但步骤复杂、需要共享较多中间状态时，应使用 Handler 处理，避免在 Service 中堆积过多业务逻辑，也避免在多个方法之间反复传递大量参数。

## 3. 总体架构

```text
问答推理链路：

用户问题
  -> MedNoteAgent 编排层
  -> 意图识别
  -> 问题改写
  -> 知识检索
  -> 证据聚合与排序
  -> 医学安全校验
  -> 回答生成
  -> 回答校验
  -> 最终响应

知识入库链路：

说明书解析
  -> 结构化抽取
  -> 索引构建
  -> 药物说明书知识库
```

系统分为两条主链路：

1. **知识入库链路**：说明书文件进入系统后，完成解析、清洗、结构化抽取、切片、向量化和索引。
2. **问答推理链路**：用户提问进入 Agent 后，完成意图识别、检索、证据聚合、安全判断和回答生成。

## 4. 推荐包结构

```text
src/main/java/org/med/note
├── agent/
│   ├── MedNoteAgent.java
│   ├── AgentContext.java
│   ├── AgentRequest.java
│   ├── AgentResponse.java
│   └── AgentStep.java
├── client/
│   ├── ModelClient.java
│   └── QianwenClient.java
├── config/
│   ├── AgentConfig.java
│   └── ModelConfig.java
├── domain/
│   ├── Drug.java
│   ├── DrugInstruction.java
│   ├── EvidenceChunk.java
│   ├── MedicalRisk.java
│   └── UserQuestion.java
├── dto/
│   ├── AskRequest.java
│   └── AskResponse.java
├── exception/
│   ├── AgentException.java
│   ├── KnowledgeNotFoundException.java
│   └── ModelCallException.java
├── handler/
│   ├── MedicalQuestionAnswerHandler.java
│   ├── InstructionIngestionHandler.java
│   ├── EvidenceRetrievalHandler.java
│   └── KnowledgeGraphWriteHandler.java
├── ingestion/
│   ├── DocumentParser.java
│   ├── InstructionExtractor.java
│   ├── KnowledgeIndexer.java
│   └── impl/
├── repository/
│   ├── DrugInstructionRepository.java
│   ├── EvidenceRepository.java
│   └── VectorRepository.java
├── service/
│   ├── DrugKnowledgeService.java
│   ├── EvidenceService.java
│   ├── MedicalAnswerService.java
│   └── RiskAssessmentService.java
├── strategy/
│   ├── AnswerStrategy.java
│   ├── IntentRecognitionStrategy.java
│   ├── QueryRewriteStrategy.java
│   ├── RetrievalStrategy.java
│   └── RiskAssessmentStrategy.java
└── prompt/
    ├── PromptTemplate.java
    └── PromptTemplateRepository.java
```

## 5. 核心模块职责

### 5.1 Agent 编排层

`MedNoteAgent` 是问答流程的统一入口，只做高层编排。对于“问答处理”这种目标明确但步骤复杂的能力，应委托 `MedicalQuestionAnswerHandler` 执行具体流程。

职责：

- 接收用户问题和上下文。
- 创建 `AgentContext`。
- 按步骤调用意图识别、问题改写、检索、证据聚合、安全判断和回答生成。
- 收集每一步的中间结果，便于调试和审计。
- 返回结构化 `AgentResponse`。
- 为复杂功能选择合适 Handler，并传入入口请求。

不负责：

- 不直接调用 DashScope SDK。
- 不直接读取 PDF。
- 不直接操作向量库。
- 不直接拼接复杂 Prompt。
- 不把意图识别、检索、风险评估、回答生成等细节都写在 Agent 类中。

### 5.2 Handler 处理层

`handler` 包承载聚焦复杂功能的完整处理过程。Handler 可以保存本次执行共享的上下文，例如 `AgentContext`、检索结果、风险结果、回答草稿等，避免每个私有方法都传递大量参数。

适合的 Handler：

- `MedicalQuestionAnswerHandler`：处理一次用户问答。
- `InstructionIngestionHandler`：处理一次说明书入库。
- `EvidenceRetrievalHandler`：处理复杂证据检索、过滤、重排。
- `KnowledgeGraphWriteHandler`：处理实体归一、节点合并、关系写入和证据绑定。

设计要求：

- Handler 只保存单次请求的中间状态，不保存跨请求可变状态。
- Handler 的入口方法应清晰，例如 `handle(AgentRequest request)`。
- Handler 内部可以按步骤拆分私有方法，例如 `recognizeIntent`、`retrieveEvidence`、`assessRisk`。
- 如果 Handler 作为 Spring 单例 Bean，应避免使用实例字段保存请求状态，改为方法内创建上下文；如果确实需要实例字段，应使用原型作用域或每次新建 Handler。
- Handler 可以调用 `service`、`repository`、`client`、`strategy`，但不应替代底层仓储或外部客户端。

推荐伪代码：

```java
/**
 * 医学说明书问答处理器。
 *
 * <p>封装单次问答的完整执行过程，内部共享 AgentContext，
 * 避免在各步骤之间反复传递问题、证据、风险和回答草稿。</p>
 */
public class MedicalQuestionAnswerHandler {

    private AgentContext context;

    public AgentResponse handle(AgentRequest request) {
        this.context = AgentContext.from(request);
        recognizeIntent();
        rewriteQuery();
        retrieveEvidence();
        assessRisk();
        generateAnswer();
        validateAnswer();
        return context.toResponse();
    }
}
```

### 5.3 知识入库模块

知识入库模块负责把药品说明书变成可检索、可追溯的知识。完整入库流程建议由 `InstructionIngestionHandler` 组织，`DocumentParser`、`InstructionExtractor`、`KnowledgeIndexer` 等组件只负责各自步骤。

核心步骤：

1. `DocumentParser`：读取 PDF、Word、文本等文件，输出原始文本。
2. `InstructionExtractor`：抽取药品名称、成分、适应症、禁忌、不良反应、相互作用、特殊人群用药等章节。
3. `KnowledgeIndexer`：将结构化结果切片，生成向量和关键词索引。
4. `DrugInstructionRepository`：保存完整说明书结构化数据。
5. `EvidenceRepository`：保存可引用的证据片段。

### 5.4 意图识别模块

`IntentRecognitionStrategy` 负责判断用户问题类型。

建议的意图枚举：

```text
CONTRAINDICATION        # 禁忌
DRUG_INTERACTION        # 药物相互作用
ADVERSE_REACTION        # 不良反应
DOSAGE_ADVICE           # 用法用量建议
PHARMACOLOGY            # 药理作用
SPECIAL_POPULATION      # 儿童、孕妇、老人、肝肾功能异常等
GENERAL_QA              # 普通说明书问答
UNKNOWN                 # 无法识别
```

意图识别结果会影响：

- 检索章节范围
- 安全校验等级
- 回答模板
- 是否要求用户补充信息

### 5.5 检索模块

`RetrievalStrategy` 负责从知识库中找出与问题最相关的证据。

推荐支持三类检索：

1. **关键词检索**：适合药名、成分名、章节名等精确匹配。
2. **向量检索**：适合自然语言问题和语义相似匹配。
3. **混合检索**：综合关键词分数、向量相似度、章节权重和药品匹配度。

检索结果统一封装为 `EvidenceChunk`，至少包含：

- `chunkId`
- `drugName`
- `sectionName`
- `content`
- `sourceDocument`
- `score`
- `metadata`

### 5.6 证据聚合模块

`EvidenceService` 对外表达证据能力，复杂的检索、过滤、重排、证据不足判断可以下沉到 `EvidenceRetrievalHandler`。

职责：

- 去重。
- 按药品、章节、可信度排序。
- 控制证据数量，避免 Prompt 过长。
- 标记冲突证据。
- 判断是否证据不足。

当证据不足时，不应强行回答，应返回“当前知识库没有足够依据”的保守结果。

### 5.7 风险评估模块

`RiskAssessmentService` 对外表达风险评估能力，具体风险识别流程可以由 Handler 组织多个 `RiskAssessmentStrategy` 完成。

高风险触发条件示例：

- 用户询问“能不能一起吃”。
- 问题中出现孕妇、儿童、老人、哺乳期、肝肾功能异常。
- 检索证据命中禁忌、慎用、严重不良反应。
- 多药联合使用但缺少完整药品信息。
- 用户描述明显不良反应或急症。

风险等级建议：

```text
LOW       # 常规信息查询
MEDIUM    # 需要谨慎解释
HIGH      # 涉及禁忌、冲突、特殊人群或严重风险
UNKNOWN   # 信息不足，不能判断
```

### 5.8 回答生成模块

`MedicalAnswerService` 负责提供回答生成能力。它依赖模型客户端，但不直接依赖具体模型 SDK。复杂问答流程不应全部写在 Service 中，而应由 `MedicalQuestionAnswerHandler` 组织上下文、证据、风险和回答策略。

回答必须包含：

- 直接结论
- 依据说明
- 风险提示
- 可执行建议
- 证据来源
- 信息不足时的追问

对于高风险问题，回答风格应更保守：

- 不替代医生诊断。
- 不给出超说明书的明确用药指令。
- 建议咨询医生或药师。
- 明确指出缺少哪些关键信息。

### 5.9 回答校验模块

`AnswerValidator` 用于在输出前检查回答质量。

校验项：

- 是否引用了证据。
- 是否出现证据中没有的药品、剂量或禁忌。
- 高风险问题是否包含安全提示。
- 证据不足时是否避免确定性结论。
- 是否泄露系统 Prompt 或内部调试信息。

## 6. 核心接口设计

### 6.1 Agent 入口

```java
/**
 * 药品说明书问答 Agent。
 *
 * <p>负责组织完整问答流程，不承载具体检索、模型调用和说明书解析细节。</p>
 */
public interface Agent {

    /**
     * 根据用户问题生成基于说明书证据的回答。
     *
     * @param request Agent 请求，包含用户问题、会话信息和可选上下文
     * @return Agent 响应，包含最终回答、证据和风险等级
     */
    AgentResponse ask(AgentRequest request);
}
```

### 6.2 模型客户端

```java
/**
 * 大模型调用抽象。
 *
 * <p>业务层只依赖该接口，具体模型由 Qianwen、OpenAI 或本地模型实现。</p>
 */
public interface ModelClient {

    /**
     * 发起单轮模型调用。
     *
     * @param request 模型请求
     * @return 模型响应
     */
    ModelResponse chat(ModelRequest request);
}
```

### 6.3 检索策略

```java
/**
 * 知识检索策略。
 */
public interface RetrievalStrategy {

    /**
     * 根据 Agent 上下文检索相关说明书证据。
     *
     * @param context Agent 上下文
     * @return 相关证据列表
     */
    List<EvidenceChunk> retrieve(AgentContext context);
}
```

### 6.4 风险评估策略

```java
/**
 * 医学风险评估策略。
 */
public interface RiskAssessmentStrategy {

    /**
     * 根据用户问题和检索证据评估回答风险。
     *
     * @param context Agent 上下文
     * @return 风险评估结果
     */
    MedicalRisk assess(AgentContext context);
}
```

### 6.5 问答 Handler

```java
/**
 * 单次医学问答处理器。
 *
 * <p>用于承载问答流程中的共享状态和复杂步骤，避免 Service 中出现过长业务流程。</p>
 */
public interface QuestionAnswerHandler {

    /**
     * 处理一次用户问答请求。
     *
     * @param request Agent 请求
     * @return Agent 响应
     */
    AgentResponse handle(AgentRequest request);
}
```

### 6.6 说明书入库 Handler

```java
/**
 * 单次说明书入库处理器。
 *
 * <p>封装 PDF 解析、章节识别、证据切片、结构化抽取和写入流程。</p>
 */
public interface InstructionIngestionHandler {

    /**
     * 处理一次说明书入库请求。
     *
     * @param request 入库请求
     * @return 入库报告
     */
    IngestionReport handle(IngestionRequest request);
}
```

## 7. Agent 上下文设计

`AgentContext` 用于串联整个流程，保存每一步的输入和输出。它通常由 `MedicalQuestionAnswerHandler` 持有和更新，`Agent` 与 `service` 不直接暴露该对象。

建议字段：

```text
requestId               # 请求 ID
sessionId               # 会话 ID
question                # 用户原始问题
normalizedQuestion      # 标准化问题
intent                  # 问题意图
mentionedDrugs          # 问题中识别出的药品
mentionedSymptoms       # 问题中识别出的症状
evidenceChunks          # 检索到的证据
risk                    # 风险评估结果
answerDraft             # 模型生成的回答草稿
finalAnswer             # 校验后的最终回答
traceSteps              # Agent 执行轨迹
metadata                # 扩展信息
```

设计要求：

- `AgentContext` 不应暴露给外部接口层。
- 每个步骤只读取自己需要的字段，并写入自己的结果。
- 上下文应可序列化，方便后续记录审计日志。
- 上下文属于单次请求，不得作为跨请求缓存使用。

## 8. 问答主流程

问答主流程建议由 `MedicalQuestionAnswerHandler` 执行，`MedNoteAgent` 只负责接收请求并调用 Handler。

```text
1. 接收 AgentRequest
2. 参数校验
3. 创建 AgentContext
4. 标准化用户问题
5. 识别问题意图
6. 识别药品名、成分名、特殊人群、症状和合并用药
7. 根据意图改写检索查询
8. 执行混合检索
9. 聚合、去重、排序证据
10. 判断证据是否充足
11. 评估医学风险等级
12. 选择回答策略和 Prompt 模板
13. 调用模型生成回答
14. 校验回答是否忠于证据
15. 返回 AgentResponse
```

## 9. 知识入库主流程

知识入库主流程建议由 `InstructionIngestionHandler` 执行，避免把 PDF 解析、章节识别、切片、抽取、写入等细节堆在 `service` 中。

```text
1. 接收说明书文件
2. 解析文件文本
3. 清洗页眉、页脚、目录、乱码和重复内容
4. 识别说明书章节
5. 抽取结构化字段
6. 按章节和语义切片
7. 生成关键词索引
8. 生成向量索引
9. 保存原文、结构化字段和证据片段
10. 记录入库版本和来源
```

建议优先抽取的章节：

- 药品名称
- 成分
- 性状
- 功能主治或适应症
- 规格
- 用法用量
- 不良反应
- 禁忌
- 注意事项
- 药物相互作用
- 药理毒理
- 特殊人群用药
- 贮藏
- 有效期

## 10. 回答结构设计

建议 `AgentResponse` 返回结构化结果，而不只是字符串。

```text
answer                  # 给用户展示的自然语言回答
intent                  # 问题意图
riskLevel               # 风险等级
evidenceList            # 引用证据
suggestedQuestions      # 建议追问
needMoreInfo            # 是否需要补充信息
missingInfo             # 缺失的信息
traceId                 # 请求追踪 ID
```

示例回答格式：

```text
结论：
根据当前知识库中的说明书信息，XXX 药物在 YYY 情况下需要谨慎使用。

依据：
1. 《XXX 说明书》禁忌章节提到……
2. 《XXX 说明书》药物相互作用章节提到……

建议：
如果你正在同时使用 A 和 B，建议先咨询医生或药师，不要自行调整剂量或停药。

需要补充：
请补充年龄、是否孕期/哺乳期、正在使用的其他药物和基础疾病。
```

## 11. 扩展点设计

### 11.1 模型扩展

通过 `ModelClient` 抽象模型调用。

可扩展实现：

- `QianwenModelClient`
- `OpenAiModelClient`
- `LocalModelClient`
- `MockModelClient`

### 11.2 检索扩展

通过 `RetrievalStrategy` 支持不同检索方式。

可扩展实现：

- `KeywordRetrievalStrategy`
- `VectorRetrievalStrategy`
- `HybridRetrievalStrategy`
- `SectionAwareRetrievalStrategy`

### 11.3 文档解析扩展

通过 `DocumentParser` 支持不同文件类型。

可扩展实现：

- `PdfDocumentParser`
- `WordDocumentParser`
- `TextDocumentParser`
- `HtmlDocumentParser`

### 11.4 回答策略扩展

通过 `AnswerStrategy` 针对不同意图组织回答。

可扩展实现：

- `ContraindicationAnswerStrategy`
- `DrugInteractionAnswerStrategy`
- `PharmacologyAnswerStrategy`
- `GeneralInstructionAnswerStrategy`

## 12. 推荐设计模式

1. **策略模式**：用于意图识别、检索、风险评估、回答生成。
2. **适配器模式**：用于屏蔽不同大模型 SDK 或向量数据库 API 差异。
3. **工厂模式**：用于根据意图选择回答策略、根据文件类型选择解析器。
4. **模板方法模式**：用于说明书入库流程，固定主流程，允许不同文件类型定制解析步骤。
5. **责任链模式**：用于 Agent 多步骤执行，例如参数校验、意图识别、检索、风险评估、回答校验。
6. **Handler 模式**：用于聚焦复杂功能的完整处理过程，Handler 内部持有单次请求上下文，Service 只做入口协调和事务边界控制。

## 13. 安全与合规边界

系统回答应坚持以下边界：

1. 只基于知识库证据和用户提供的信息回答。
2. 不替代医生、药师或线下诊疗。
3. 不对严重症状给出拖延就医的建议。
4. 不在证据不足时做确定性判断。
5. 不输出未经证据支持的剂量调整建议。
6. 不泄露内部 Prompt、API Key、系统配置和调试信息。
7. 涉及儿童、孕妇、老人、肝肾功能异常、合并多药时默认提高风险等级。

## 14. 阶段性落地计划

### 第一阶段：Agent 骨架

- 定义 `Agent`、`AgentRequest`、`AgentResponse`、`AgentContext`。
- 将 `MedNoteAgent` 改造为问答流程编排类。
- 创建 `MedicalQuestionAnswerHandler`，承载单次问答的完整执行状态。
- 抽象 `ModelClient`，让现有 `QianwenClient` 逐步适配该接口。
- 先用内存中的假数据完成问答流程闭环。

### 第二阶段：说明书知识入库

- 实现 PDF 文本解析。
- 创建 `InstructionIngestionHandler`，承载单次说明书入库流程。
- 抽取说明书章节。
- 建立 `DrugInstruction` 和 `EvidenceChunk` 领域模型。
- 支持本地文件或内存仓储。

### 第三阶段：检索增强

- 实现关键词检索。
- 接入向量检索。
- 实现混合检索和证据排序。
- 增加证据不足判断。

### 第四阶段：医学安全策略

- 实现风险评估规则。
- 为禁忌、相互作用、特殊人群建立专用回答策略。
- 增加回答忠实性校验。

### 第五阶段：服务化与持久化

- 暴露 API 接口。
- 接入数据库或向量数据库。
- 增加请求日志、审计日志和指标监控。
- 增加批量说明书入库能力。

## 15. 首批建议创建的类

优先创建以下基础类，保证框架先跑通：

```text
agent/
├── Agent.java
├── MedNoteAgent.java
├── AgentRequest.java
├── AgentResponse.java
└── AgentContext.java

handler/
├── MedicalQuestionAnswerHandler.java
├── InstructionIngestionHandler.java
├── EvidenceRetrievalHandler.java
└── KnowledgeGraphWriteHandler.java

domain/
├── EvidenceChunk.java
├── MedicalIntent.java
└── MedicalRisk.java

client/
├── ModelClient.java
├── ModelRequest.java
└── ModelResponse.java

strategy/
├── IntentRecognitionStrategy.java
├── RetrievalStrategy.java
├── RiskAssessmentStrategy.java
└── AnswerStrategy.java
```

## 16. 后续编码建议

1. 先不要一次性接入完整向量库，先用接口和内存实现跑通架构。
2. 先让每个步骤输出可观察的中间结果，方便定位 Agent 回答问题。
3. Prompt 模板单独管理，不要散落在业务类中。
4. 所有回答都要带证据，不带证据的回答只能作为“无法判断”或“需要补充信息”。
5. 说明书原文、结构化字段、证据片段应保留版本号，方便以后更新和追溯。
6. `service` 保持薄入口，不承载过长业务流程；当私有方法之间需要频繁传递同一批参数时，优先抽成 Handler 并使用上下文对象共享状态。

## 关键词生成与小模型配置建议

Agent 在两个位置强依赖关键词：一是工具选择，二是知识库召回。当前 demo 通过 `request_planning` 工具一次性生成任务关键词、意图、查询目标、`queryKeywords` 和推荐说明书；检索工具使用 `queryKeywords` 参与 mock 知识库打分。生产环境建议把 `request_planning` 内部的规则替换为小模型 JSON 输出，配置示例见 `application.yml` 的 `mednote.agent.keyword.small-model`。

推荐小模型配置：

- 模型：优先选择低延迟、低成本、支持 JSON/结构化输出的小模型，例如 `qwen-turbo` 类模型。
- 温度：`0.0` 到 `0.2`，保证关键词稳定可复现。
- 输出：固定 JSON schema，至少包含 `taskKeywords`、`queryKeywords`、`toolHints`、`knowledgeKeywords`。
- 数量：每类 8 到 16 个关键词，要求去重、保留药品名/章节名/医学风险词/同义词。
- 质量约束：关键词必须能回指原文证据或标准同义词，不输出泛词，例如“这个”“说明”“问题”。

工具关键词生成：为每个工具维护静态 `keywordHints` 和 `triggers`，小模型只负责把用户任务映射到这些工具关键词，不直接决定工具执行。这样可以让工具选择可控、可解释。

Query 关键词生成：从用户问题中抽取实体、章节、症状、风险词，再补充同义词和规范表达。例如“能不能吃”应扩展为“禁忌/慎用/用法用量/过敏”。

知识关键词生成：入库时对每个证据片段离线生成关键词，至少包含药品名、章节名、原文医学词、标准同义词、适用/禁忌人群和风险标签；在线检索时用 query keywords 与 knowledge keywords 做混合召回。

## 意图识别小模型选择与输出建议

意图识别链路的目标不是“聊天”，而是快速产出可执行路由字段：用户想查什么、是否存在用药风险、应该优先召回哪些说明书。因此模型选择建议如下：

- **优先级 1：低延迟和稳定结构化输出**。首选 `qwen-turbo` 这类低成本小模型；温度建议 `0.0~0.1`；接口超时建议控制在 500~1000ms。
- **优先级 2：JSON schema 可靠性**。必须能稳定输出固定字段，字段缺失时走规则兜底，不允许直接输出自然语言解释。
- **优先级 3：医学词鲁棒性**。模型要能识别“能不能吃/是否能服用/过敏/孕妇/儿童/合并用药/肝肾功能”等口语表达，并映射到说明书章节。
- **升级策略**：常规路由使用小模型；当输入包含多药合并、特殊人群、严重不良反应或模型置信度低时，再升级到更强模型或人工/药师复核流程。

推荐结构化输出 schema：

```json
{
  "intent": "CONTRAINDICATION | ADVERSE_REACTION | DOSAGE_ADVICE | SPECIAL_POPULATION | CAUTION | INSTRUCTION_RECOMMENDATION | GENERAL_QA",
  "queryTargets": ["功能主治", "用法用量", "禁忌", "注意事项", "不良反应"],
  "medicationRiskLevel": "LOW | MEDIUM | HIGH",
  "medicationRiskSignals": ["过敏", "孕妇", "儿童", "合并用药", "肝肾功能", "不良反应"],
  "recommendedInstructions": ["二冬汤颗粒说明书", "菖麻熄风颗粒说明书"],
  "keywords": ["药品名", "章节名", "风险词", "症状词"],
  "confidence": 0.0
}
```

当前 demo 的 `request_planning` 工具已经按这个方向返回 `queryTargets`、`medicationRiskLevel`、`medicationRiskSignals` 和 `recommendedInstructions` 元数据；生产实现只需要把内部规则替换成小模型调用并保持字段兼容。


## 动态工具选择与会话排查

当前 Agent 不再只做一次性工具加入。`AgentToolPlanner` 会在每个工具执行后基于最新 `ToolContext` 重新排序候选工具；已执行工具会进入本次会话的 unloaded 列表，避免循环调用。如果新上下文产生了新的关键词、风险等级或证据，后续轮次可以追加新的工具。

排查问题时优先看三类信息：

1. `sessionId`：一次请求的唯一会话标识。
2. `metadata.toolCall`：每次工具调用的开始/结束时间、耗时、输入快照和输出元数据。
3. 最终 `message` 事件中的 `toolCalls`：完整工具调用链，可用于复现工具选择和结果合并过程。

工具粒度原则：工具应代表一个可替换能力，而不是一个私有函数。请求理解相关的关键词、意图、query 改写和说明书推荐强依赖同一批上下文，因此合并为 `request_planning`；检索、用药风险评估、回答生成则分别保留为独立工具，因为它们未来会替换为不同服务或策略。

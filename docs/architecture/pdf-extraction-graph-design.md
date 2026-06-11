# PDF 信息抽取与知识图谱节点设计

## 1. 文档目标

本文档说明 MedNoteAgent 如何从药物说明书 PDF 中提取信息，并设计可扩展的知识图谱节点、关系、写入流程和消费方式。

本项目 Markdown 文档以 IntelliJ IDEA / JetBrains 系列 IDE 的默认 Markdown 预览为主要适配目标。图结构优先使用 `text` 代码块、Markdown 表格和缩进树表达，不依赖 Mermaid 渲染。

目标场景：

- 根据药物说明书回答用户关于禁忌、建议、药理冲突、特殊人群用药、不良反应等问题。
- 回答必须有说明书原文证据支撑。
- 后续可以逐步接入向量检索、关键词检索、知识图谱推理和多药冲突判断。
- PDF 入库、图谱写入、复杂检索等聚焦但复杂的功能应使用 Handler 组织流程和共享上下文，避免在 Service 中堆积过多业务逻辑。

核心设计思想：

> 以“说明书原文证据片段”为可信基础，以“药品、成分、禁忌、相互作用、特殊人群”等实体节点构建可推理关系。

也就是说，图谱不是替代原文，而是帮助系统更快找到原文、约束回答范围、识别风险关系。

## 2. 总体链路

```text
说明书 PDF
  -> PDF 解析
  -> 文本清洗
  -> 章节识别
      -> 证据切片
          -> 向量化
          -> 证据库写入
      -> 结构化抽取
          -> 实体标准化
          -> 图谱写入
  -> Agent 问答消费

Agent 问答消费同时读取：
  - 原文证据库
  - 向量库
  - 知识图谱
```

入库链路分为 8 个步骤：

1. PDF 解析：读取 PDF 文本、页码、段落、表格。
2. 文本清洗：去除页眉、页脚、重复目录、乱码和多余空白。
3. 章节识别：识别“成分、功能主治、禁忌、注意事项、药物相互作用”等标准章节。
4. 证据切片：按章节和语义切成可引用片段。
5. 结构化抽取：从章节中抽取药品、成分、禁忌对象、不良反应、相互作用等实体。
6. 实体标准化：处理别名、同义词、规格差异和重复实体。
7. 多库写入：同时写入原文证据库、向量库和知识图谱。
8. 质量校验：检查关键章节是否缺失、实体是否可追溯、关系是否有证据片段。

这条链路建议由 `PdfInstructionIngestionHandler` 或 `InstructionIngestionHandler` 承载。Handler 内部持有本次入库的 `IngestionContext`，用于共享文档、章节、切片、实体、关系和入库报告，避免每个步骤都传递大量参数。

## 3. 为什么采用“证据片段 + 图谱实体”的双层设计

### 3.1 只用向量库的问题

如果只做向量检索，优点是实现快、语义召回好，但存在明显问题：

- 难以稳定判断“药 A 和药 B 是否冲突”。
- 对禁忌、特殊人群等强规则问题，容易召回相似但不精确的内容。
- 难以表达“某个禁忌来自哪份说明书的哪个章节”。
- 多药联合查询时，纯文本相似度不足以做关系推理。

### 3.2 只用知识图谱的问题

如果只做图谱，结构化推理能力强，但也有问题：

- PDF 说明书很多内容不是标准三元组，强行结构化会丢失语义。
- “尚不明确”“慎用”“未进行该项实验”等医学表达很难简单转成确定关系。
- 回答用户时仍然需要原文证据，否则可信度不足。

### 3.3 双层设计的效果

双层设计把两者结合：

- 向量库负责召回相关原文。
- 图谱负责实体关系、风险路径和结构化约束。
- 证据片段负责回答可追溯。

最终效果：

- 回答更稳：高风险结论必须绑定说明书证据。
- 检索更准：可先按药品、章节、关系过滤，再做语义召回。
- 可解释性更好：能告诉用户“依据来自哪份说明书、哪个章节”。
- 扩展更自然：后续可以增加疾病、症状、成分、靶点、药理分类等节点。

## 4. PDF 信息抽取设计

### 4.0 入库 Handler 设计

PDF 入库是一个目标明确但步骤复杂的功能，不建议把完整流程写在 `DrugKnowledgeService` 或 `EvidenceService` 中。推荐使用 Handler 组织单次入库流程。

职责边界：

```text
InstructionIngestionService       # 对外入口、权限校验、事务边界、任务状态
PdfInstructionIngestionHandler    # 单次 PDF 入库流程和上下文状态
DocumentParser                    # 文件解析
TextCleaner                       # 文本清洗
SectionRecognizer                 # 章节识别
EvidenceChunker                   # 证据切片
EntityExtractor                   # 结构化抽取
KnowledgeGraphWriteHandler        # 图谱节点和关系写入
Repository                        # 数据落库
```

Handler 中建议保存的上下文：

```text
request
document
cleanDocument
sections
evidenceChunks
extractedEntities
extractedRelations
writeResult
qualityReport
```

为什么这样设计：

- PDF 入库步骤多，且每一步都依赖前面产生的中间结果。
- 使用 Handler 可以减少长参数列表，让每个私有方法聚焦一个步骤。
- Service 保持轻量，主要表达业务入口，而不是承载解析和写入细节。
- 后续增加 OCR、人工复核、失败重试时，可以在 Handler 内扩展流程，不影响对外 Service 接口。

注意：

- Handler 中的类属性只代表单次处理状态，不能作为跨请求缓存。
- 如果使用 Spring 单例 Bean，不要在实例字段中保存请求级状态；可以改为方法内 `IngestionContext context`，或使用原型作用域。
- Handler 可以调用 parser、strategy、repository、client，但不应直接绕过 repository 写底层存储。

### 4.1 PDF 解析层

建议定义接口：

```java
/**
 * 说明书文档解析器。
 *
 * <p>负责从不同格式文件中提取文本、页码和基础版面信息。</p>
 */
public interface DocumentParser {

    /**
     * 解析说明书文件。
     *
     * @param documentPath 文件路径
     * @return 文档解析结果
     */
    ParsedDocument parse(String documentPath);
}
```

解析结果建议包含：

```text
documentId          # 文档唯一 ID
fileName            # 文件名
fileHash            # 文件哈希，用于幂等和版本判断
pages               # 每页文本
paragraphs          # 段落列表
tables              # 表格列表
metadata            # PDF 元数据
parseQuality        # 解析质量评分
```

### 4.2 文本清洗层

清洗目标：

- 删除重复页眉、页脚、页码。
- 修复异常换行。
- 合并被 PDF 拆散的词句。
- 保留章节标题和列表层级。
- 保留页码映射，方便证据追溯。

清洗后不要覆盖原文，应保留：

- `rawText`：原始解析文本。
- `cleanText`：清洗后的可处理文本。
- `pageRange`：对应页码范围。

原因：后续如果用户质疑回答，需要能追溯到原始 PDF。

### 4.3 章节识别层

药品说明书通常有相对固定的章节，应优先基于规则识别，再用模型兜底。

常见章节：

```text
药品名称
成分
性状
功能主治 / 适应症
规格
用法用量
不良反应
禁忌
注意事项
药物相互作用
药理毒理
临床试验
贮藏
包装
有效期
执行标准
批准文号
生产企业
```

章节识别结果：

```text
sectionId
documentId
drugName
sectionName
standardSectionCode
content
startPage
endPage
confidence
```

`standardSectionCode` 用于统一章节名称，例如：

```text
CONTRAINDICATION
ADVERSE_REACTION
DRUG_INTERACTION
PHARMACOLOGY
DOSAGE
PRECAUTION
SPECIAL_POPULATION
```

### 4.4 证据切片层

证据切片不要只按固定字数切，应优先按章节和语义切。

推荐规则：

- 一个切片只属于一个药品和一个章节。
- 禁忌、相互作用、特殊人群等高风险章节尽量小切片。
- 药理毒理、临床试验等长章节可以稍大切片。
- 切片必须保留章节名、页码、原文位置和来源文档。
- 切片之间可以保留少量重叠，避免上下文断裂。

证据片段字段：

```text
chunkId
documentId
drugId
drugName
sectionId
sectionName
sectionCode
content
startPage
endPage
position
sourceFile
contentHash
embeddingId
createdAt
```

### 4.5 结构化抽取层

结构化抽取负责把说明书内容转成实体和关系。

抽取方式建议分三层：

1. 规则抽取：章节标题、批准文号、药品名称、规格等格式稳定信息。
2. 词典抽取：药名、成分、疾病、症状、特殊人群等可用词典匹配。
3. 模型抽取：禁忌对象、相互作用描述、风险等级、建议动作等复杂语义。

模型抽取必须要求返回 JSON，并带证据原文：

```json
{
  "drugName": "示例药品",
  "entities": [
    {
      "type": "CONTRAINDICATION",
      "name": "孕妇",
      "evidence": "孕妇禁用。",
      "confidence": 0.92
    }
  ],
  "relations": [
    {
      "source": "示例药品",
      "type": "CONTRAINDICATED_FOR",
      "target": "孕妇",
      "evidence": "孕妇禁用。",
      "confidence": 0.92
    }
  ]
}
```

## 5. 知识图谱节点设计

### 5.1 节点总览

```text
Document
  -> HAS_SECTION -> Section
      -> HAS_CHUNK -> EvidenceChunk

Drug
  -> DESCRIBED_BY -> Document
  -> HAS_SECTION -> Section
  -> HAS_INGREDIENT -> Ingredient
  -> INDICATED_FOR -> Indication
  -> CONTRAINDICATED_FOR -> Contraindication
  -> HAS_ADVERSE_REACTION -> AdverseReaction
  -> CAUTION_FOR -> Population
  -> HAS_PHARMACOLOGY -> Pharmacology
  -> PARTICIPATES_IN -> DrugInteraction

Ingredient
  -> PARTICIPATES_IN -> DrugInteraction

EvidenceChunk
  -> SUPPORTS -> Contraindication
  -> SUPPORTS -> AdverseReaction
  -> SUPPORTS -> Population
  -> SUPPORTS -> DrugInteraction
```

| 起点节点 | 关系 | 终点节点 | 说明 |
| --- | --- | --- | --- |
| Document | HAS_SECTION | Section | 说明书包含章节 |
| Section | HAS_CHUNK | EvidenceChunk | 章节拆分为可引用证据片段 |
| Drug | DESCRIBED_BY | Document | 药品由说明书描述 |
| Drug | HAS_INGREDIENT | Ingredient | 药品包含成分 |
| Drug | INDICATED_FOR | Indication | 药品适应症或功能主治 |
| Drug | CONTRAINDICATED_FOR | Contraindication | 药品禁忌对象或条件 |
| Drug | HAS_ADVERSE_REACTION | AdverseReaction | 药品不良反应 |
| Drug | CAUTION_FOR | Population | 特殊人群慎用或注意 |
| Drug | HAS_PHARMACOLOGY | Pharmacology | 药理作用 |
| Drug / Ingredient | PARTICIPATES_IN | DrugInteraction | 药品或成分参与相互作用事件 |
| EvidenceChunk | SUPPORTS | 医学实体或关系 | 原文证据支撑结构化结论 |

核心节点类型：

```text
Document              # 说明书文档
Drug                  # 药品
Section               # 说明书章节
EvidenceChunk         # 证据片段
Ingredient            # 成分
Indication            # 适应症 / 功能主治
Contraindication      # 禁忌对象或禁忌条件
AdverseReaction       # 不良反应
DrugInteraction       # 药物相互作用事件
Population            # 特殊人群
Disease               # 疾病
Symptom               # 症状
Pharmacology          # 药理作用
Dosage                # 用法用量
Manufacturer          # 生产企业
Approval              # 批准文号 / 注册信息
```

### 5.2 Document 节点

表示一份说明书 PDF。

字段：

```text
documentId
fileName
fileHash
sourcePath
drugName
approvalNumber
version
parseStatus
createdAt
updatedAt
```

为什么需要：

- 解决同一药品多版本说明书的问题。
- 支持重复导入幂等判断。
- 支持回答时引用来源文档。

### 5.3 Drug 节点

表示药品实体。

字段：

```text
drugId
standardName
commonName
tradeName
pinyin
drugType
approvalNumber
manufacturer
status
```

为什么需要：

- 用户问题通常围绕药名。
- 药品存在通用名、商品名、别名，需要统一归一。
- 多份说明书可能指向同一药品或不同厂家版本。

### 5.4 Section 节点

表示说明书中的标准章节。

字段：

```text
sectionId
documentId
drugId
sectionName
sectionCode
contentHash
startPage
endPage
confidence
```

为什么需要：

- 禁忌、相互作用等回答需要优先定位特定章节。
- 章节可以作为检索过滤条件，提高准确率。
- 同一药品不同章节的风险等级不同。

### 5.5 EvidenceChunk 节点

表示可以引用的原文证据片段。

字段：

```text
chunkId
documentId
drugId
sectionId
sectionCode
content
contentHash
startPage
endPage
embeddingId
qualityScore
```

为什么需要：

- 它是回答可信度的基础。
- 所有结构化关系都应能追溯到至少一个证据片段。
- 向量检索的最小召回单元应是证据片段，而不是整份 PDF。

### 5.6 Ingredient 节点

表示药品成分。

字段：

```text
ingredientId
standardName
alias
category
source
```

为什么需要：

- 药物相互作用经常发生在成分层面，而不是商品名层面。
- 中成药、复方制剂可能包含多个成分。
- 后续可扩展到成分禁忌、成分相似性和成分冲突。

### 5.7 Contraindication 节点

表示禁忌条件、禁忌人群或禁忌疾病。

字段：

```text
contraindicationId
name
targetType          # POPULATION / DISEASE / SYMPTOM / INGREDIENT / CONDITION
severity            # HIGH / MEDIUM / LOW / UNKNOWN
normalizedName
description
```

为什么需要：

- 用户常问“我这种情况能不能吃”。
- 禁忌对象需要结构化，方便快速判断。
- 禁忌和慎用要区分，不应混为一个结论。

### 5.8 DrugInteraction 节点

表示药物相互作用事件，而不是简单地只连两种药。

字段：

```text
interactionId
interactionType     # ENHANCE / REDUCE / TOXICITY / UNKNOWN
riskLevel
mechanism
recommendation
description
```

为什么不只用 `Drug -> Drug` 边：

- 两个药之间可能存在多个相互作用机制。
- 相互作用可能来自成分、药理作用或代谢通路。
- 需要记录风险等级、机制和建议，这些更适合作为事件节点。

推荐结构：

```text
Drug A --PARTICIPATES_IN--> DrugInteraction <--PARTICIPATES_IN-- Drug B
DrugInteraction --SUPPORTED_BY--> EvidenceChunk
```

### 5.9 Population 节点

表示特殊人群。

示例：

```text
儿童
孕妇
哺乳期妇女
老年人
肝功能不全患者
肾功能不全患者
过敏体质者
```

字段：

```text
populationId
name
normalizedName
category
```

为什么需要：

- 特殊人群是医学安全判断的重要触发条件。
- 用户自然语言中经常出现“老人能不能吃”“孕妇可以用吗”。

### 5.10 Pharmacology 节点

表示药理作用、药效机制或毒理信息。

字段：

```text
pharmacologyId
name
mechanism
effect
description
```

为什么需要：

- 药理冲突不仅来自说明书的“药物相互作用”章节，也可能来自药理作用相反或叠加。
- 后续可以根据药理作用做更深层的冲突提示。

## 6. 关系设计

### 6.1 基础来源关系

```text
Document -HAS_SECTION-> Section
Section -HAS_CHUNK-> EvidenceChunk
Drug -DESCRIBED_BY-> Document
Drug -HAS_SECTION-> Section
EvidenceChunk -SOURCE_OF-> Entity
```

用途：

- 建立原文追溯链路。
- 从任意实体都能回到说明书和证据片段。

### 6.2 药品结构关系

```text
Drug -HAS_INGREDIENT-> Ingredient
Drug -HAS_DOSAGE-> Dosage
Drug -HAS_APPROVAL-> Approval
Drug -MANUFACTURED_BY-> Manufacturer
```

用途：

- 回答“这个药是什么成分”“批准文号是什么”“怎么服用”等基础问题。

### 6.3 医学语义关系

```text
Drug -INDICATED_FOR-> Indication
Drug -CONTRAINDICATED_FOR-> Contraindication
Drug -CAUTION_FOR-> Population
Drug -HAS_ADVERSE_REACTION-> AdverseReaction
Drug -HAS_PHARMACOLOGY-> Pharmacology
Drug -HAS_PRECAUTION-> Precaution
DrugInteraction -SUPPORTED_BY-> EvidenceChunk
```

用途：

- 支持禁忌、注意事项、不良反应、药理作用等问答。

### 6.4 相互作用关系

推荐使用事件节点：

```text
Drug -PARTICIPATES_IN-> DrugInteraction
Ingredient -PARTICIPATES_IN-> DrugInteraction
DrugInteraction -HAS_RISK_LEVEL-> RiskLevel
DrugInteraction -SUPPORTED_BY-> EvidenceChunk
```

消费示例：

- 用户问“药 A 和药 B 能一起吃吗？”
- 系统先找到 Drug A、Drug B。
- 查询两者是否参与同一个 `DrugInteraction`。
- 如果没有直接关系，再查成分层关系。
- 最后回到 EvidenceChunk 获取原文依据。

## 7. 数据写入设计

### 7.1 写入目标

一次 PDF 入库应写入三类数据：

```text
1. 原文证据库：保存文档、章节、证据片段。
2. 向量库：保存 EvidenceChunk 的 embedding。
3. 知识图谱：保存实体节点和关系。
```

推荐先以本地文件或数据库抽象接口实现，后续再替换具体存储。

写入流程建议由 `KnowledgeGraphWriteHandler` 承载。Service 只调用 Handler 并处理事务、重试和状态更新；节点合并、关系创建、证据绑定等细节放在 Handler 内。

### 7.2 写入顺序

```text
1. 计算 PDF 文件哈希。
2. 判断 Document 是否已存在。
3. 解析 PDF，生成 ParsedDocument。
4. 清洗文本，生成 CleanDocument。
5. 识别章节，写入 Section。
6. 生成 EvidenceChunk，写入证据库。
7. 对 EvidenceChunk 生成 embedding，写入向量库。
8. 从章节和切片中抽取实体和关系。
9. 标准化实体。
10. 合并或创建图谱节点。
11. 创建关系，并绑定 EvidenceChunk。
12. 更新 Document 入库状态。
13. 记录入库报告。
```

对应 Handler 步骤：

```text
PdfInstructionIngestionHandler
├── prepareDocument()
├── parseDocument()
├── cleanText()
├── recognizeSections()
├── buildEvidenceChunks()
├── extractEntitiesAndRelations()
├── writeEvidenceAndVectors()
├── writeKnowledgeGraph()
└── buildReport()

KnowledgeGraphWriteHandler
├── normalizeEntities()
├── mergeNodes()
├── createRelations()
├── bindEvidenceChunks()
└── validateGraphWrite()
```

### 7.3 幂等写入

必须保证同一 PDF 重复导入不会生成重复节点。

建议唯一键：

```text
Document: fileHash
Drug: standardName + approvalNumber + manufacturer
Section: documentId + sectionCode + contentHash
EvidenceChunk: documentId + sectionId + contentHash
Ingredient: normalizedName
Population: normalizedName
Contraindication: normalizedName + targetType
DrugInteraction: participantIds + interactionType + evidenceHash
```

### 7.4 事务策略

写入过程建议使用“分阶段状态”而不是一次性全成功。

Document 状态：

```text
PENDING
PARSING
SECTION_EXTRACTED
CHUNKED
INDEXED
GRAPH_WRITTEN
FAILED
```

这样设计的原因：

- PDF 解析、模型抽取、向量化都可能失败。
- 分阶段状态方便重试。
- 可以避免半成品数据被问答链路消费。

问答消费时只读取 `GRAPH_WRITTEN` 或 `INDEXED` 且质量合格的文档。

## 8. Agent 如何消费这些数据

复杂消费链路也可以使用 Handler，例如 `MedicalQuestionAnswerHandler` 或 `EvidenceRetrievalHandler`。Service 负责提供“问答能力”入口，Handler 负责单次问答中的实体识别、图谱查询、证据召回、重排和缺失信息判断。

### 8.1 用户问禁忌

问题：

```text
孕妇能不能服用二冬汤颗粒？
```

消费流程：

```text
1. 识别药品：二冬汤颗粒。
2. 识别人群：孕妇。
3. 查询 Drug -> CONTRAINDICATED_FOR / CAUTION_FOR -> Population。
4. 召回禁忌、注意事项、特殊人群章节的 EvidenceChunk。
5. 如果命中明确禁忌，输出高风险结论。
6. 如果只命中慎用或尚不明确，输出保守建议。
7. 返回证据来源。
```

### 8.2 用户问药物冲突

问题：

```text
菖麻熄风颗粒能和二冬汤颗粒一起吃吗？
```

消费流程：

```text
1. 识别两个药品。
2. 查询 DrugInteraction 直接关系。
3. 查询两个药品的 Ingredient 是否存在相互作用。
4. 查询两个药品的药理作用是否存在叠加或相反风险。
5. 召回“药物相互作用”“注意事项”“禁忌”章节证据。
6. 如果证据不足，不能直接说可以同服。
7. 输出“当前说明书证据不足以判断 + 建议咨询医生或药师”。
```

### 8.3 用户问不良反应

问题：

```text
吃这个药头晕正常吗？
```

消费流程：

```text
1. 识别症状：头晕。
2. 如果上下文无药名，先追问药品名称。
3. 查询 Drug -> HAS_ADVERSE_REACTION -> Symptom / AdverseReaction。
4. 召回不良反应章节证据。
5. 如果说明书提到类似反应，说明依据。
6. 如果症状严重或持续，输出就医建议。
```

### 8.4 用户问药理作用

问题：

```text
这个药为什么能止咳？
```

消费流程：

```text
1. 识别药品和问题意图：PHARMACOLOGY。
2. 查询 Drug -> HAS_PHARMACOLOGY。
3. 召回药理毒理、功能主治、成分章节证据。
4. 生成面向普通用户的解释。
5. 避免扩展到说明书没有支持的机制。
```

## 9. 数据消费时的回答策略

### 9.1 强结论必须满足条件

只有同时满足以下条件，才能输出较明确结论：

```text
1. 识别到明确药品。
2. 召回到高相关证据。
3. 图谱关系和原文证据一致。
4. 证据来自禁忌、相互作用、注意事项等对应章节。
5. 没有与之冲突的证据。
```

### 9.2 证据不足时的回答方式

不应回答：

```text
可以一起吃。
```

应回答：

```text
当前知识库中没有检索到足够的说明书依据证明两者可以或不可以同服。由于涉及合并用药，建议提供正在使用的其他药物、年龄、基础疾病，并咨询医生或药师。
```

### 9.3 回答中必须带证据

建议响应结构：

```text
answer
riskLevel
evidenceList
missingInfo
suggestedQuestions
traceId
```

`evidenceList` 至少包含：

```text
drugName
sectionName
content
sourceDocument
pageRange
chunkId
```

## 10. 质量校验设计

### 10.1 入库质量校验

每份 PDF 入库后生成报告：

```text
documentId
drugName
parseQuality
sectionCount
missingRequiredSections
chunkCount
entityCount
relationCount
failedReasons
```

必须检查：

- 是否识别到药品名称。
- 是否识别到核心章节。
- 每个实体关系是否绑定证据。
- 是否存在大量空章节或乱码。
- 证据片段是否过长或过短。

### 10.2 抽取质量校验

模型抽取结果必须校验：

- JSON 是否合法。
- 实体类型是否在枚举范围内。
- 关系类型是否在白名单内。
- 证据文本是否能在原文中找到。
- 置信度低于阈值的结果不直接入图，进入人工复核或低可信队列。

## 11. 常见坑与规避方案

### 11.1 PDF 解析乱码

问题：

- 扫描版 PDF 没有文本层。
- 字体编码异常导致中文乱码。
- 表格和多栏排版顺序错乱。

规避：

- 解析后计算 `parseQuality`。
- 低质量文档进入 OCR 流程。
- 表格单独解析，不要简单拼成普通段落。
- 保留页码和原始文本，方便排查。

### 11.2 章节识别不稳定

问题：

- 有的说明书叫“功能主治”，有的叫“适应症”。
- 标题可能带编号，例如“一、成份”。
- PDF 换行会拆开标题。

规避：

- 建立标准章节映射表。
- 规则识别优先，模型兜底。
- 每个章节保存 `confidence`。
- 缺失核心章节时生成入库警告。

### 11.3 “尚不明确”被误判为安全

问题：

说明书中常见：

```text
药物相互作用：尚不明确。
```

这不代表没有相互作用，只代表说明书没有明确证据。

规避：

- 将“尚不明确”标记为 `UNKNOWN`，不能转成 `SAFE`。
- 回答时输出“说明书未明确说明”，而不是“没有风险”。

### 11.4 禁忌和慎用混淆

问题：

“禁用”“忌用”“慎用”“不宜”“应在医师指导下使用”风险程度不同。

规避：

- 关系类型区分：

```text
CONTRAINDICATED_FOR
CAUTION_FOR
NOT_RECOMMENDED_FOR
USE_UNDER_GUIDANCE
UNKNOWN
```

- 回答时根据关系类型输出不同语气。

### 11.5 实体标准化困难

问题：

- 同一药品有通用名、商品名、别名。
- 成分有繁简体、别名、旧名。
- 疾病和症状表达不统一。

规避：

- 所有实体都保存 `name` 和 `normalizedName`。
- 建立别名表。
- 对无法确认的实体保留原文名称，不强行合并。

### 11.6 图谱关系膨胀

问题：

如果把所有短语都建成节点，图谱会迅速变乱。

规避：

- 只把可复用、可查询、可推理的对象建成节点。
- 普通描述保留在 EvidenceChunk 中。
- 低置信度关系不直接写入主图。

### 11.7 切片过大或过小

问题：

- 切片过大：检索命中后 Prompt 太长，答案不聚焦。
- 切片过小：上下文断裂，模型误解。

规避：

- 按章节语义切片。
- 高风险章节小切片。
- 长章节保留父子切片关系。
- 切片带章节标题和前后少量上下文。

### 11.8 模型抽取幻觉

问题：

模型可能抽取原文不存在的禁忌、成分或建议。

规避：

- 要求模型返回证据原文。
- 校验证据必须能在原文中匹配。
- 低置信度结果进入复核。
- 关系写入必须绑定 EvidenceChunk。

### 11.9 多版本说明书冲突

问题：

同一药品不同厂家、不同版本说明书可能内容不同。

规避：

- `Document` 节点必须保存版本、批准文号、厂家。
- 回答时说明依据来自哪份说明书。
- 不同版本证据冲突时，提示“不同说明书信息存在差异”。

### 11.10 问答消费误用图谱

问题：

图谱中没有关系，不代表现实中没有风险。

规避：

- 没查到关系只能说明“当前知识库未检索到明确证据”。
- 不允许回答“绝对安全”。
- 合并用药、特殊人群默认提高风险等级。

## 12. 推荐落地步骤

### 第一阶段：PDF 到证据片段

- 创建 `PdfInstructionIngestionHandler`，用它组织单次 PDF 入库流程。
- 实现 `DocumentParser`。
- 实现章节识别。
- 实现 `EvidenceChunk` 切片。
- 将证据片段写入本地 JSON 或数据库。
- 支持根据药名和章节关键词检索证据。

### 第二阶段：基础图谱

- 创建 `KnowledgeGraphWriteHandler`，用它组织节点合并、关系创建和证据绑定。
- 建立 `Document`、`Drug`、`Section`、`EvidenceChunk` 四类基础节点。
- 建立 `HAS_SECTION`、`HAS_CHUNK`、`DESCRIBED_BY` 基础关系。
- 保证所有回答都能引用证据。

### 第三阶段：医学实体抽取

- 抽取 `Ingredient`、`Contraindication`、`AdverseReaction`、`Population`。
- 建立禁忌、慎用、不良反应关系。
- 增加抽取结果校验。

### 第四阶段：相互作用与药理冲突

- 引入 `DrugInteraction` 事件节点。
- 支持药品层和成分层冲突查询。
- 引入风险等级和建议动作。

### 第五阶段：混合检索和 Agent 消费

- 对 EvidenceChunk 建向量索引。
- Agent 问答时同时使用图谱过滤、关键词召回和向量召回。
- 输出带证据、风险等级和缺失信息的结构化回答。

## 13. 首批建议实现的接口

```text
handler/
├── PdfInstructionIngestionHandler.java
├── KnowledgeGraphWriteHandler.java
├── EvidenceRetrievalHandler.java
└── MedicalQuestionAnswerHandler.java

ingestion/
├── DocumentParser.java
├── TextCleaner.java
├── SectionRecognizer.java
├── EvidenceChunker.java
├── EntityExtractor.java
└── KnowledgeGraphWriter.java

domain/
├── ParsedDocument.java
├── InstructionSection.java
├── EvidenceChunk.java
├── ExtractedEntity.java
└── ExtractedRelation.java

repository/
├── DocumentRepository.java
├── EvidenceRepository.java
├── VectorRepository.java
└── GraphRepository.java
```

## 14. 最小可行版本设计

如果先做一个可运行版本，建议不要一开始就上完整图数据库。

最小版本可以这样做：

```text
1. 用 JSON 或关系型表保存 Document、Section、EvidenceChunk。
2. 用简单 Map 保存 Drug、Ingredient、Contraindication 等节点。
3. 用关系表保存 sourceId、relationType、targetId、evidenceChunkId。
4. 先做关键词检索，后续再补向量检索。
5. Agent 回答必须带 chunkId 和原文片段。
6. 入库和问答复杂流程先使用 Handler 管理上下文，Service 只保留轻量入口。
```

这样做的好处：

- 先验证 PDF 抽取质量和问答闭环。
- 不被图数据库选型卡住。
- 后续可以平滑迁移到 Neo4j、NebulaGraph 或其他图存储。

## 15. 最重要的约束

1. 没有证据片段，不写入医学关系。
2. 没有说明书依据，不输出确定性医学结论。
3. “尚不明确”不是“安全”。
4. 图谱关系必须能回溯到 `Document -> Section -> EvidenceChunk`。
5. 药物相互作用优先建事件节点，不要只建药品到药品的简单边。
6. 同一 PDF 重复导入必须幂等。
7. 入库状态未完成的数据不能进入问答消费链路。
8. Handler 只能保存单次处理状态，不能保存跨请求可变状态。
9. 当 Service 中出现过长流程、长参数列表或大量临时状态时，应优先拆到聚焦功能的 Handler。

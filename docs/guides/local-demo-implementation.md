# MedNoteAgent 本地 Demo 快速实现方案

## 1. 文档目标

本文档说明如何在本地快速实现 MedNoteAgent Demo。目标不是一次性完成完整生产系统，而是用最少依赖跑通核心链路：

```text
说明书文件
  -> 本地解析或文本导入
  -> 章节识别
  -> 证据切片
  -> SQLite 存储
  -> 本地检索
  -> 规则风险判断
  -> 模板回答或可选模型回答
```

Demo 的原则：

1. 不引入 Spring、图数据库、向量数据库、消息队列等复杂依赖。
2. 数据库使用本地 SQLite 文件。
3. 文件、索引、日志、配置都放在项目本地目录。
4. 先保证可运行、可调试、可追溯，再逐步替换为更强组件。
5. 大模型调用作为可选能力，默认可以用规则模板回答跑通流程。

## 2. Demo 范围

### 2.1 Demo 要实现什么

第一版 Demo 建议只实现以下能力：

| 能力 | 说明 |
| --- | --- |
| 文档导入 | 从本地 `docs/reference/drug-instructions/` 或 `data/input/` 读取说明书文本 |
| 章节识别 | 识别常见说明书章节，例如成分、功能主治、禁忌、注意事项、不良反应 |
| 证据切片 | 按章节切分为可引用片段 |
| 本地存储 | 使用 SQLite 保存文档、章节、证据片段 |
| 本地检索 | 根据药名、章节名、关键词检索证据 |
| 风险判断 | 根据问题和命中章节判断风险等级 |
| 回答生成 | 用模板生成有依据的回答 |
| 命令行交互 | 使用 `main` 方法运行导入和提问 |

### 2.2 Demo 暂不实现什么

第一版不建议实现：

1. 不接入真实向量数据库。
2. 不接入 Neo4j 等图数据库。
3. 不做复杂 OCR。
4. 不做复杂前端页面。
5. 不做多用户、登录、权限、任务队列。
6. 不强依赖大模型，避免 API Key、网络和费用影响本地演示。

这些能力后续可以在接口稳定后逐步替换。

## 3. 推荐技术选型

### 3.1 最小依赖版

| 类型 | 选择 | 说明 |
| --- | --- | --- |
| 语言 | Java | 沿用当前 Maven 项目 |
| 构建 | Maven | 当前项目已使用 Maven |
| 数据库 | SQLite | 单文件数据库，适合本地 Demo |
| 文档格式 | `.txt` / `.md` | 最快跑通，不先处理 PDF 复杂解析 |
| 检索 | Java 规则评分 + SQLite 查询 | 不引入 Elasticsearch、向量库 |
| 回答 | 本地模板 | 不依赖模型也能演示 |
| 模型 | 可选 DashScope 千问 | 当前项目已有 `QianwenClient`，但 Demo 默认关闭 |

### 3.2 需要新增的 Maven 依赖

Demo 最少只需要新增 SQLite JDBC：

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.46.1.0</version>
</dependency>
```

如果第一版就要直接读取 PDF，可以再加 PDFBox：

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.31</version>
</dependency>
```

建议：

1. 第一版先不加 PDFBox，把 PDF 说明书手动或用外部工具转成 `.txt`，先跑通链路。
2. 第二版再加 PDFBox，把 PDF 解析纳入程序。

## 4. 本地目录规划

建议在项目根目录增加以下本地目录：

```text
data/
├── input/                  # 待导入的说明书 txt/md/pdf
├── output/                 # 导入报告、回答记录
├── db/
│   └── mednote-demo.db     # SQLite 数据库文件
└── logs/                   # 本地日志
```

说明：

1. `data/input` 放说明书文本。
2. `data/db/mednote-demo.db` 是 Demo 的全部数据库。
3. `data/output` 保存每次导入报告和问答结果，方便调试。
4. `data/` 可以加入 `.gitignore`，避免提交本地运行数据。

## 5. Demo 数据库设计

第一版只需要 4 张表。

### 5.1 document

保存说明书文件。

```sql
CREATE TABLE IF NOT EXISTS document (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_hash TEXT NOT NULL,
    drug_name TEXT,
    import_time TEXT NOT NULL,
    status TEXT NOT NULL
);
```

### 5.2 section

保存说明书章节。

```sql
CREATE TABLE IF NOT EXISTS section (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER NOT NULL,
    section_name TEXT NOT NULL,
    section_code TEXT NOT NULL,
    content TEXT NOT NULL,
    start_order INTEGER NOT NULL,
    FOREIGN KEY(document_id) REFERENCES document(id)
);
```

### 5.3 evidence_chunk

保存可引用证据片段。

```sql
CREATE TABLE IF NOT EXISTS evidence_chunk (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER NOT NULL,
    section_id INTEGER NOT NULL,
    drug_name TEXT,
    section_name TEXT NOT NULL,
    section_code TEXT NOT NULL,
    content TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    score REAL DEFAULT 0,
    FOREIGN KEY(document_id) REFERENCES document(id),
    FOREIGN KEY(section_id) REFERENCES section(id)
);
```

### 5.4 qa_log

保存本地问答记录，方便回看。

```sql
CREATE TABLE IF NOT EXISTS qa_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    risk_level TEXT NOT NULL,
    evidence_ids TEXT,
    created_at TEXT NOT NULL
);
```

### 5.5 可选关系表

如果想演示“轻量知识图谱”，可以在 SQLite 中用一张关系表替代图数据库：

```sql
CREATE TABLE IF NOT EXISTS knowledge_relation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_type TEXT NOT NULL,
    source_name TEXT NOT NULL,
    relation_type TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_name TEXT NOT NULL,
    evidence_chunk_id INTEGER NOT NULL,
    confidence REAL DEFAULT 1.0,
    FOREIGN KEY(evidence_chunk_id) REFERENCES evidence_chunk(id)
);
```

第一版可以先不实现这张表，只保留接口位置。

## 6. 推荐包结构

在当前项目基础上，建议 Demo 先使用下面这些包：

```text
src/main/java/org/med/note
├── demo/
│   ├── DemoApplication.java              # 命令行入口
│   ├── DemoCommand.java                  # import / ask 命令分发
│   └── DemoConfig.java                   # 本地路径和开关
├── ingestion/
│   ├── LocalInstructionImporter.java     # 本地说明书导入
│   ├── TextDocumentParser.java           # txt/md 解析
│   ├── PdfDocumentParser.java            # 可选，PDFBox 解析
│   ├── SectionRecognizer.java            # 章节识别
│   └── EvidenceChunker.java              # 证据切片
├── repository/
│   ├── SqliteConnectionFactory.java      # SQLite 连接
│   ├── DatabaseInitializer.java          # 建表
│   ├── DocumentRepository.java
│   ├── SectionRepository.java
│   └── EvidenceChunkRepository.java
├── qa/
│   ├── LocalQuestionAnswerService.java   # 本地问答主流程
│   ├── KeywordRetriever.java             # 关键词检索
│   ├── RiskAssessor.java                 # 风险判断
│   └── TemplateAnswerGenerator.java      # 模板回答
└── domain/
    ├── DocumentInfo.java
    ├── SectionInfo.java
    ├── EvidenceChunk.java
    ├── SearchResult.java
    └── AnswerResult.java
```

Demo 阶段不需要一开始就做完所有正式架构，但类名和职责应尽量贴近后续正式版本，避免重写。

## 7. 核心流程设计

### 7.1 初始化数据库

启动 Demo 时先初始化 SQLite：

```text
DemoApplication
  -> DatabaseInitializer.init()
      -> 创建 data/db 目录
      -> 连接 mednote-demo.db
      -> 执行 CREATE TABLE IF NOT EXISTS
```

### 7.2 导入说明书

导入命令：

```text
mvn exec:java -Dexec.mainClass=org.med.note.demo.DemoApplication -Dexec.args="import data/input"
```

导入流程：

```text
LocalInstructionImporter.importDirectory(inputDir)
  -> 遍历 txt/md 文件
  -> 计算文件 hash
  -> 判断是否已导入
  -> TextDocumentParser 读取文本
  -> SectionRecognizer 识别章节
  -> EvidenceChunker 切片
  -> 写入 document / section / evidence_chunk
  -> 输出导入报告
```

### 7.3 用户提问

提问命令：

```text
mvn exec:java -Dexec.mainClass=org.med.note.demo.DemoApplication -Dexec.args="ask 二冬汤颗粒有什么禁忌"
```

问答流程：

```text
LocalQuestionAnswerService.answer(question)
  -> 识别问题关键词
  -> 识别意图和章节偏好
  -> KeywordRetriever 检索证据
  -> RiskAssessor 判断风险等级
  -> TemplateAnswerGenerator 生成回答
  -> 保存 qa_log
  -> 控制台输出结果
```

## 8. 章节识别策略

Demo 阶段使用规则识别即可。

常见章节标题：

```text
药品名称
成分
性状
功能主治
适应症
规格
用法用量
不良反应
禁忌
注意事项
药物相互作用
药理毒理
贮藏
包装
有效期
执行标准
批准文号
生产企业
```

章节识别方法：

1. 按行读取文本。
2. 去掉多余空白。
3. 如果某一行匹配标准章节标题，则开启新章节。
4. 后续内容累积到当前章节。
5. 文档结束时保存最后一个章节。

章节编码建议：

| 章节名 | section_code |
| --- | --- |
| 禁忌 | CONTRAINDICATION |
| 不良反应 | ADVERSE_REACTION |
| 注意事项 | PRECAUTION |
| 药物相互作用 | DRUG_INTERACTION |
| 用法用量 | DOSAGE |
| 功能主治 / 适应症 | INDICATION |
| 药理毒理 | PHARMACOLOGY |
| 成分 | INGREDIENT |
| 其他 | GENERAL |

## 9. 证据切片策略

第一版切片不需要复杂语义算法。

推荐规则：

1. 每个切片只属于一个章节。
2. 短章节直接作为一个切片。
3. 长章节按段落切分。
4. 如果单个片段超过 800 字，可以按句号、分号、换行继续拆分。
5. 每个切片记录 `document_id`、`section_id`、`drug_name`、`section_name`、`section_code`、`content_hash`。

切片大小建议：

| 章节类型 | 建议大小 |
| --- | --- |
| 禁忌 | 100 - 400 字 |
| 注意事项 | 200 - 600 字 |
| 不良反应 | 200 - 600 字 |
| 药物相互作用 | 100 - 400 字 |
| 药理毒理 | 400 - 1000 字 |
| 其他章节 | 300 - 800 字 |

## 10. 本地检索策略

Demo 不需要向量库。先实现可解释的关键词检索。

### 10.1 问题意图到章节映射

| 问题关键词 | 优先章节 |
| --- | --- |
| 禁忌、不能吃、能不能吃、不适合 | CONTRAINDICATION, PRECAUTION |
| 不良反应、副作用、难受 | ADVERSE_REACTION, PRECAUTION |
| 怎么吃、用量、一天几次 | DOSAGE |
| 一起吃、合用、相互作用、冲突 | DRUG_INTERACTION, PRECAUTION |
| 孕妇、儿童、老人、哺乳期 | PRECAUTION, CONTRAINDICATION |
| 成分、含有什么 | INGREDIENT |
| 治什么、适应症、功能主治 | INDICATION |
| 药理、机制 | PHARMACOLOGY |

### 10.2 检索评分

可以用简单加权评分：

```text
总分 =
  药品名称命中分
  + 章节偏好分
  + 问题关键词命中分
  + 内容关键词命中分
```

建议权重：

| 命中项 | 分数 |
| --- | --- |
| 药品名称命中 | +50 |
| 优先章节命中 | +30 |
| 章节名直接命中 | +20 |
| 每个关键词命中 | +5 |
| 内容包含“禁用、慎用、尚不明确、严重”等风险词 | +10 |

返回前 3 到 5 条证据即可。

## 11. 风险判断策略

本地 Demo 使用规则判断风险。

### 11.1 高风险关键词

```text
禁用
禁忌
慎用
孕妇
儿童
老人
哺乳期
肝功能
肾功能
过敏
相互作用
合用
不良反应
严重
急症
```

### 11.2 风险等级规则

| 条件 | 风险等级 |
| --- | --- |
| 命中禁忌章节，且内容包含“禁用” | HIGH |
| 问题涉及合并用药或相互作用 | HIGH |
| 问题涉及孕妇、儿童、老人、肝肾功能异常 | HIGH |
| 命中注意事项、不良反应章节 | MEDIUM |
| 只查询成分、批准文号、生产企业 | LOW |
| 没有检索到证据 | UNKNOWN |

## 12. 回答生成策略

### 12.1 默认使用模板回答

Demo 默认不调用大模型，使用模板生成回答：

```text
结论：
根据当前本地说明书证据，{结论}

依据：
1. {证据片段 1}
2. {证据片段 2}

风险提示：
{根据风险等级生成提示}

建议：
本回答仅基于已导入说明书，不替代医生或药师建议。若涉及特殊人群、合并用药或症状加重，请咨询专业人员。

证据来源：
- 药品：{drug_name}
- 章节：{section_name}
- 证据 ID：{chunk_id}
```

### 12.2 可选接入模型

如果需要更自然的表达，可以在模板回答前增加模型生成，但要保留证据约束：

```text
system:
你是药品说明书问答助手。只能基于用户提供的证据回答。
如果证据不足，必须说明无法判断，不能编造。

user:
问题：{question}

证据：
{evidence_chunks}

请输出：结论、依据、风险提示、建议、证据来源。
```

Demo 中建议用配置开关控制：

```text
demo.model.enabled=false
demo.model.provider=qianwen
```

如果 `demo.model.enabled=false`，系统只走本地模板。

## 13. 配置建议

不引入 Spring 时，可以使用简单的 `.properties` 文件：

```text
config/demo.properties
```

内容示例：

```properties
demo.db.path=data/db/mednote-demo.db
demo.input.dir=data/input
demo.output.dir=data/output
demo.model.enabled=false
demo.model.name=qwen-max
demo.model.api-key-env=DASHSCOPE_API_KEY
```

注意：

1. API Key 不写入配置文件，只写环境变量名。
2. 如果没有配置文件，代码使用默认值。
3. 本地 Demo 不需要配置中心。

## 14. 命令行交互设计

建议支持 4 个命令。

### 14.1 初始化

```text
mvn exec:java -Dexec.mainClass=org.med.note.demo.DemoApplication -Dexec.args="init"
```

效果：

```text
初始化 data 目录
初始化 SQLite 数据库
创建基础表
```

### 14.2 导入

```text
mvn exec:java -Dexec.mainClass=org.med.note.demo.DemoApplication -Dexec.args="import data/input"
```

效果：

```text
导入 data/input 下的说明书文本
输出导入数量、章节数量、证据片段数量
```

### 14.3 提问

```text
mvn exec:java -Dexec.mainClass=org.med.note.demo.DemoApplication -Dexec.args="ask 二冬汤颗粒有什么禁忌"
```

效果：

```text
输出回答、风险等级、证据来源
```

### 14.4 查看数据

```text
mvn exec:java -Dexec.mainClass=org.med.note.demo.DemoApplication -Dexec.args="stats"
```

效果：

```text
文档数量：2
章节数量：30
证据片段数量：80
问答记录数量：5
```

## 15. 最快落地步骤

### 第 1 步：先准备文本

把说明书 PDF 内容转成 `.txt`，放入：

```text
data/input/
```

文件名建议：

```text
二冬汤颗粒_CXZS2500013.txt
菖麻熄风颗粒_CXZS2500020.txt
```

第一版不要被 PDF 解析卡住。只要文本格式大致保留章节标题，就能先验证问答链路。

### 第 2 步：接入 SQLite

新增 SQLite JDBC 依赖，实现：

```text
SqliteConnectionFactory
DatabaseInitializer
```

先确保本地能创建：

```text
data/db/mednote-demo.db
```

### 第 3 步：实现导入

实现：

```text
TextDocumentParser
SectionRecognizer
EvidenceChunker
LocalInstructionImporter
```

验收标准：

```text
导入 1 份 txt 后，document / section / evidence_chunk 表有数据。
```

### 第 4 步：实现检索

实现：

```text
KeywordRetriever
```

验收标准：

```text
输入“禁忌”“不良反应”“用法用量”等问题，可以返回对应章节证据。
```

### 第 5 步：实现回答

实现：

```text
RiskAssessor
TemplateAnswerGenerator
LocalQuestionAnswerService
```

验收标准：

```text
输入一个问题后，控制台输出结论、依据、风险提示、建议和证据来源。
```

### 第 6 步：再考虑 PDF 和模型

在主链路可运行后，再增加：

1. `PdfDocumentParser`，使用 PDFBox 直接读取 PDF。
2. `ModelAnswerGenerator`，可选调用千问生成更自然回答。
3. `knowledge_relation`，用 SQLite 表模拟知识图谱关系。

## 16. Demo 成功标准

本地 Demo 完成后，应能演示以下场景：

1. 本地初始化 SQLite 数据库。
2. 导入 1 到 2 份药品说明书文本。
3. 自动识别主要章节。
4. 自动生成证据片段。
5. 用户输入“这个药有什么禁忌”可以检索到禁忌章节。
6. 用户输入“孕妇能不能吃”可以触发高风险提示。
7. 用户输入“这个药有什么成分”可以返回成分证据。
8. 所有回答都包含证据 ID 和章节来源。
9. 没有证据时明确说明“当前本地知识库没有足够依据”。

## 17. 后续升级路线

Demo 跑通后，可以按下面顺序升级：

| 升级项 | 替换前 | 替换后 |
| --- | --- | --- |
| PDF 解析 | 手动 txt | PDFBox / OCR |
| 检索 | 关键词规则 | BM25 / 向量检索 / 混合检索 |
| 存储 | SQLite | PostgreSQL / MySQL |
| 图谱 | SQLite relation 表 | Neo4j / NebulaGraph |
| 回答 | 模板回答 | 大模型证据约束回答 |
| 交互 | 命令行 | REST API / Web 页面 |
| 部署 | 本地运行 | Docker / 服务化部署 |

关键点是：正式能力只替换实现，不推翻 Demo 的接口和流程。

## 18. 推荐第一版任务拆分

| 任务 | 文件或类 | 预计结果 |
| --- | --- | --- |
| 初始化数据目录 | `DemoConfig` | 自动创建 `data/` |
| 初始化数据库 | `DatabaseInitializer` | 创建 SQLite 表 |
| 读取文本 | `TextDocumentParser` | 返回完整文本 |
| 识别章节 | `SectionRecognizer` | 返回章节列表 |
| 生成切片 | `EvidenceChunker` | 返回证据片段 |
| 写入数据库 | `DocumentRepository` 等 | SQLite 有数据 |
| 检索证据 | `KeywordRetriever` | 返回 TopN 证据 |
| 风险判断 | `RiskAssessor` | 返回 LOW / MEDIUM / HIGH / UNKNOWN |
| 生成回答 | `TemplateAnswerGenerator` | 输出结构化回答 |
| 命令行入口 | `DemoApplication` | 支持 init / import / ask / stats |

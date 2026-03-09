# AI 面试模拟助手 (Java + LangChain4j)

这是一个基于 Spring Boot 3 和 LangChain4j 构建的智能 AI 面试模拟后端系统。系统利用 RAG（检索增强生成）技术，结合 PDF 的官方题库和用户个人简历，为候选人提供动态、多轮次、拟真度极高的流式 (SSE) AI 面试体验。

---

## 🚀 项目特性

- **多模型支持**：支持 DeepSeek、Qwen（本地 Ollama）、阿里百炼等多家大模型无缝切换。
- **动态 RAG 检索**：基于 Pinecone 或本地 ChromaDB 的向量检索引擎，动态结合"官方面试题库"和"用户个人简历"生成精准面试发问。
- **简历解析提取**：上传 PDF 格式简历，系统自动持久化、向量化，并使用 LLM 萃取技术栈与工作年限，定制专属面试。
- **向量库热插拔**：代码在底层利用 `EmbeddingStoreProvider` 实现了极度解耦，只需改动 `application.properties` 的一行配置，即可在 Pinecone 与 ChromaDB 间灵活切换。
- **流式输出体验**：基于 `Flux` 响应式的 Server-Sent Events (SSE)，实现类似 ChatGPT 端打字机般丝滑回复。

---

## 🛠️ 环境准备与依赖

在运行项目前，请确保您的系统中已安装并开启以下服务：

1. **JDK 17 或以上** (Maven 3.8+ 编译环境)
2. **MySQL 8.x** (存储面试会话流水、答题记录、简历元数据)
3. **Redis** (分布式缓存及其他预留项)
4. **MongoDB** (存储 LangChain4j 的 `ChatMemory` 多轮对话记忆上下文)
5. **向量数据库 (二选一)**：
   - [Pinecone](https://www.pinecone.io/) (云端，需注册获取 API Key)
   - [ChromaDB](https://www.trychroma.com/) (本地，可使用 Docker 快速运行，默认端口 8000)
6. **LLM API Key** (如阿里百炼 DashScope 的 API Key)

---

## ⚙️ 第一步：配置说明与启动

项目采用**环境变量优先**的敏感信息保护策略，以防止密码泄露：

### 1. 配置环境变量 (Environment Variables)

启动前，请在系统环境变量或 IDE (如 IntelliJ IDEA) 的 Run Configuration 中配置以下值：

```properties
DASH_SCOPE_API_KEY=你的阿里百炼模型_API_KEY
PINECONE_API_KEY=你的PINECONE_向量库_API_KEY (如果使用Pinecone)
DB_PASSWORD=你的MySQL密码
REDIS_PASSWORD=你的Redis密码
MONGO_PASSWORD=你的MongoDB密码 (如果设置了密码)
```
*(注意：如果不配置，系统在本地配置 `application-local.properties` 中预留了部分默认占位符，但建议配齐以防连接失败)*

### 2. 初始化数据库

在运行程序之前，请在您的 MySQL 中执行项目根目录 `schema.sql` (位于 `src/main/resources/sql/schema.sql` 下)：
这会为你建立三张关键表格：`interview_session`, `interview_question`, `user_resume`。

### 3. 选择向量数据库 (Pinecone vs ChromaDB)

打开 `src/main/resources/application.properties`，找到如下配置：

```properties
# 选择向量数据库实现 (支持 pinecone 或 chroma)
vector.store.type=pinecone

# 如果使用 ChromaDB，请配置本地 Chroma 服务地址 (需提前 docker run chromadb)
chroma.baseUrl=http://localhost:8000
```
- 如选用 **Pinecone**：只需给定环境变量 `PINECONE_API_KEY`。
- 如选用本地 **ChromaDB**：将 `vector.store.type` 改为 `chroma`，并确保本机跑了 Chroma 服务。

### 4. 运行项目

执行以下 Maven 命令编译并运行项目：
```bash
mvn clean compile
mvn spring-boot:run
```
*(或者直接在 IDE 中启动 `JavaAiLangchain4jApplication` 主类)*。

启动成功后，Swagger 接口文档地址：`http://localhost:8080/doc.html` (基于 Knife4j)。

---

## 📖 第二步：API 核心流程与使用指引

这是一场完整的 AI 面试周期，请按照以下接口顺序进行接口调用 (可通过 Postman 或同类型工具)：

### 1. (可选) 上传个性化简历
- **接口**：`POST /api/interview/resume/upload`
- **说明**：上传你的个人 PDF 简历。服务器将提取技术栈，并把简历写入向量库 `user-resume-{userId}` 的专属命名空间下。
- **参数 (form-data)**：
  - `file`: PDF 面试简历文件
  - `userId`: 测试用户ID (自定义，如 "u123")
- **返回**：会返回解析出的技能栈（Skills）和年份等信息。

### 2. 开始建立面试会话
- **接口**：`POST /api/interview/start`
- **说明**：初始化这场面试的环境变量，并获取 `sessionId`。
- **请求体 JSON**：
  ```json
  {
    "userId": "u123",                    // 如果上传了简历请保持一致，它会作为检索凭证
    "position": "java-backend",          // 职位：如 java-backend, frontend 等
    "difficulty": "medium",              // 难度：easy, medium, hard
    "totalQuestions": 5                  // 面试预计提问数
  }
  ```
- **返回**：提取返回中的 `sessionId` 供后续核心交互使用。

### 3. 提交面试回答并获取 AI 回击 (核心 SSE)
- **接口**：`POST /api/interview/answer`
- **说明**：这是核心交互接口。由于是 Server-Sent Events 流式输出，你能"流式"看到 AI 面试官对你答案的点评以及提出的"下一个问题"。该接口自带 RAG，会自动调取第一步你传的简历或后端的知识库 PDF 进行专业提问。
- **请求体 JSON**：
  ```json
  {
    "sessionId": "填写上一步的sessionId",
    "answer": "你好，面试官，我是xxx。关于Spring Boot的核心原理，我认为是约定大于配置..."
  }
  ```
- **注意**：前端调用此接口请使用支持 `text/event-stream` 格式的客户端（如 `EventSource` 或 `@microsoft/fetch-event-source`库），以接受打字机效果。每次的回答都会被 MySQL 持久化，ChatMemory 也会同步记录到 MongoDB，AI 将拥有长久记忆。

### 4. (面试结束后) 获取综合评估结果
- **接口**：`GET /api/interview/result/{sessionId}`
- **说明**：此接口读取本场面试的总题目数量和详细记录，可用于在面板上向学员展示最终打分与评价。

---

## 📁 进阶：如何增加官方知识库题图？

如果你想让 AI 系统面试官变得更聪明，能提出你公司独有的题目：
1. 请将包含专属题库的 PDF 文件放至项目的 `src/main/resources/content` 目录下。
2. 在 `application.properties` 中开启题目自动导入开关：
   ```properties
   interview.questions.auto-load=true
   ```
3. 下次 Spring Boot 启动时，`QuestionDataLoader` 就会自动解析所有的 PDF 并灌入你配置的向量库的 "official" 命名空间中！**注意：导入完毕后建议将其改回 `false` 以免每次重启发生无效花费（Pinecone 按量计费）**。

# AI 面试系统接口文档与完整流程设计

## 一、系统概述

本系统是一个基于 AI 的技术面试模拟平台，通过五个核心接口实现从简历上传、面试开始、问题回答到结果评估的完整面试流程。

---

## 二、核心接口列表

### 1. 上传简历接口
**接口路径**: `POST /api/interview/resume/upload`

**功能描述**: 上传并解析候选人简历，提取技能栈和工作年限，向量化存储到向量数据库

**请求参数**:
```json
{
  "file": "MultipartFile (PDF 格式简历文件)",
  "userId": "String (用户 ID)"
}
```

**返回数据**:
```json
{
  "id": 1,
  "userId": "user123",
  "resumePath": "uploads/resumes/xxx_resume.pdf",
  "skills": "Java, Spring Boot, MySQL, Redis",
  "experienceYears": 3
}
```

**使用时机**: 面试开始前，用户首次上传简历时调用

**业务处理**:
- 保存 PDF 文件到本地存储
- 使用 AI 提取简历中的技能栈和工作年限
- 将简历内容分块并向量化，存储到 Pinecone 向量数据库（使用用户隔离的 namespace）
- 将简历元数据保存到 `user_resume` 表

---

### 2. 开始面试接口
**接口路径**: `POST /api/interview/start`

**功能描述**: 创建面试会话，初始化面试状态

**请求参数**:
```json
{
  "userId": "String (用户 ID)",
  "position": "String (岗位类型：java-backend/frontend/fullstack)",
  "difficulty": "String (难度：easy/medium/hard/expert)"
}
```

**返回数据**:
```json
{
  "sessionId": "uuid-1234-5678",
  "userId": "user123",
  "position": "java-backend",
  "difficulty": "medium",
  "status": "ongoing",
  "startTime": "2024-01-01T10:00:00",
  "message": "面试已开始，请准备好回答问题！"
}
```

**使用时机**: 用户选择好岗位和难度后，点击"开始面试"按钮调用

**业务处理**:
- 生成唯一的 sessionId (UUID)
- 创建 `InterviewSession` 记录并保存到 `interview_session` 表
- 初始化面试状态为"ongoing"
- 返回 sessionId 供后续接口使用

---

### 3. 获取面试问题接口
**接口路径**: `GET /api/interview/question?sessionId={sessionId}`

**功能描述**: 获取当前应该向候选人展示的问题

**请求参数**:
- `sessionId`: 面试会话 ID

**返回数据**:
```json
{
  "sessionId": "uuid-1234-5678",
  "questionNumber": 1,
  "question": "你好！我是你的面试官，请简单介绍一下你自己，并说说你对 Java 后端开发岗位的理解？",
  "category": "java-backend",
  "difficulty": "medium"
}
```

**使用时机**: 
- 开始面试后立即调用获取第一个问题
- 每次回答完问题后调用获取下一个问题

**业务处理**:
- 根据 sessionId 查询面试会话
- 根据已答题数判断当前是第几个问题
- 第一个问题返回自我介绍引导语
- 后续问题从 AI Agent 动态生成（TODO：结合知识库）

---

### 4. 提交答案接口（流式返回）
**接口路径**: `POST /api/interview/answer`

**功能描述**: 提交候选人的回答，AI 实时评估并流式返回反馈

**请求参数**:
```json
{
  "sessionId": "String (会话 ID)",
  "answer": "String (用户的回答内容)"
}
```

**返回数据** (Server-Sent Events 流式返回):
```
【评估】回答基本正确，对 Spring 的理解比较到位
【反馈】可以进一步补充 Spring AOP 的实现原理...
【下一步】好的，接下来我们聊聊数据库相关的问题...
```

**使用时机**: 候选人回答完每个问题后立即调用

**业务处理**:
- 保存用户答案到 `interview_question` 表
- 更新会话的已答题数
- 调用 RAG 从知识库检索相关参考资料
- 调用 AI Agent 进行评估和追问
- 流式返回 AI 的评估、反馈和下一步指示

**关键点**:
- 使用 SSE (Server-Sent Events) 实现流式输出，提升用户体验
- RAG 检索用户简历和岗位知识库，实现个性化面试
- AI Agent 根据回答质量决定是否继续追问或切换话题

---

### 5. 获取面试结果接口
**接口路径**: `GET /api/interview/result/{sessionId}`

**功能描述**: 获取面试的最终评估结果和总分

**请求参数**:
- `sessionId`: 面试会话 ID

**返回数据**:
```json
{
  "sessionId": "uuid-1234-5678",
  "score": 85.5,
  "totalQuestions": 8,
  "answeredQuestions": 8,
  "evaluation": "表现良好！基础知识掌握较好，建议加强系统设计和场景分析能力。💪",
  "endTime": "2024-01-01T10:30:00"
}
```

**使用时机**: 面试结束（用户主动结束或达到预设题数）后调用

**业务处理**:
- 查询所有答题记录
- 计算平均分
- 更新会话状态为"completed"
- 生成综合评估文本

---

## 三、完整面试流程设计

### 阶段一：面试准备

```
用户操作流程：
1. 用户进入面试页面
2. 上传 PDF 简历
   └─> 调用：POST /api/interview/resume/upload
   └─> 系统解析简历，提取技能栈，向量化存储

3. 选择面试岗位和难度
   - 岗位：Java 后端/前端/全栈
   - 难度：简单/中等/困难/专家

4. 点击"开始面试"
   └─> 调用：POST /api/interview/start
   └─> 获得 sessionId
```

### 阶段二：面试进行中

```
面试对话循环：
┌─────────────────────────────────────┐
│ 第 N 轮面试对话                      │
├─────────────────────────────────────┤
│ 1. 获取问题                         │
│    └─> GET /api/interview/question  │
│    └─> 返回："请简单介绍一下自己..." │
│                                     │
│ 2. 用户回答问题                     │
│    (语音转文字或文字输入)            │
│                                     │
│ 3. 提交答案                         │
│    └─> POST /api/interview/answer   │
│    └─> 流式返回 AI 评估和反馈         │
│                                     │
│ 4. AI 判断是否继续追问               │
│    - 如果回答不完整 → 追问           │
│    - 如果回答完整 → 切换下一个话题   │
│                                     │
│ 5. 判断是否结束面试                 │
│    - 达到预设题数 → 结束             │
│    - 用户主动结束 → 结束             │
│    - 否则 → 回到步骤 1               │
└─────────────────────────────────────┘
```

**详细对话流程示例**:

```
【开场】
用户：点击"开始面试"
系统调用：GET /api/interview/question?sessionId=xxx
AI: "你好！我是你的面试官，请简单介绍一下你自己，并说说你对 Java 后端开发岗位的理解？"

【第一轮：自我介绍】
用户："我叫张三，有 3 年 Java 开发经验，熟悉 Spring Boot、MySQL..."
系统调用：POST /api/interview/answer
  请求：{sessionId: "xxx", answer: "我叫张三..."}
  流式返回：
    "【评估】自我介绍清晰，经验与岗位匹配 👍
     【反馈】提到了 Spring Boot 和 MySQL，很好
     【下一步】好的，了解到你有 3 年经验。那我们深入聊聊 Spring 框架，请说说 Spring Bean 的生命周期？"

【第二轮：技术基础】
系统调用：GET /api/interview/question?sessionId=xxx
AI: "请说说 Spring Bean 的生命周期？"
用户："Spring Bean 的生命周期包括实例化、属性赋值、初始化..."
系统调用：POST /api/interview/answer
  流式返回：
    "【评估】回答基本正确，但可以更详细 😊
     【反馈】你提到了实例化和初始化，很好。补充一下，BeanPostProcessor 在什么时候调用？
     【追问】能说说 BeanPostProcessor 的作用吗？"
     
用户："BeanPostProcessor 是在 Bean 初始化前后调用的..."
系统调用：POST /api/interview/answer
  流式返回：
    "【评估】很好！理解到位 ✨
     【反馈】BeanPostProcessor 是 Spring 扩展点的重要组成
     【下一步】接下来我们聊聊数据库，MySQL 的索引数据结构是什么？"

【第三轮：数据库】
... (继续问答循环)

【结束面试】
用户：点击"结束面试"按钮
系统调用：GET /api/interview/result/{sessionId}
返回：
  {
    "score": 85.5,
    "evaluation": "表现良好！基础知识掌握较好，建议加强系统设计和场景分析能力。💪",
    "totalQuestions": 8
  }
```

### 阶段三：面试结束

```
结束流程：
1. 用户主动点击"结束面试"或达到预设题数 (如 10 题)

2. 调用：GET /api/interview/result/{sessionId}
   └─> 计算总分
   └─> 生成综合评估
   └─> 更新会话状态为"completed"

3. 展示面试报告
   - 总分：85.5/100
   - 答题数：8/10
   - 各项能力雷达图
   - 详细评估和建议

4. 用户可查看历史面试记录
   - 查询自己的面试记录列表
   - 查看每次面试的详细答题情况和 AI 反馈
```

---

## 四、关键技术点

### 1. 对话状态管理
- 使用 `sessionId` 标识每次面试会话
- 使用 MongoDB 存储对话历史 (通过 `MongoChatMemoryStore`)
- 使用 `MessageWindowChatMemory` 维护最近 20 条对话

### 2. RAG 知识检索
- 用户回答时，自动检索：
  - 用户简历（从 Pinecone 的 user-resume-namespace）
  - 岗位知识库（从 Pinecone 的 question-namespace）
- 检索结果提供给 AI Agent，实现个性化面试

### 3. 流式响应
- 使用 WebFlux 的 `Flux<String>` 实现 SSE 流式输出
- 用户实时看到 AI 的评估、反馈，提升交互体验

### 4. 智能追问机制
AI Agent 根据以下因素决定是否追问：
- 回答完整性（是否覆盖关键点）
- 回答准确性（是否有错误理解）
- 面试进度（是否需要切换话题）
- 难度匹配（是否符合预设难度）

---

## 五、数据表结构

### interview_session (面试会话表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(64) | 会话 ID (UUID) |
| user_id | VARCHAR(64) | 用户 ID |
| position | VARCHAR(32) | 面试岗位 |
| difficulty | VARCHAR(16) | 难度等级 |
| status | VARCHAR(16) | 状态 (ongoing/completed) |
| total_questions | INT | 总题数 |
| answered_count | INT | 已答题数 |
| score | DOUBLE | 总分 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |

### interview_question (面试题目表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(64) | 会话 ID |
| question_order | INT | 题目序号 |
| question_text | TEXT | 问题内容 |
| category | VARCHAR(32) | 分类 |
| difficulty | VARCHAR(16) | 难度 |
| user_answer | TEXT | 用户答案 |
| ai_feedback | TEXT | AI 点评 |
| score | INT | 本题得分 |
| answer_time | DATETIME | 答题时间 |

### user_resume (用户简历表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | VARCHAR(64) | 用户 ID |
| resume_path | VARCHAR(512) | 简历文件路径 |
| skills | TEXT | 技能列表 (JSON) |
| experience_years | INT | 工作年限 |
| expected_position | VARCHAR(32) | 期望职位 |
| upload_time | DATETIME | 上传时间 |

---

## 六、接口调用时序图

```
用户               前端              后端 API              AI Agent           数据库
 |                 |                    |                    |                  |
 |--上传简历-------|                    |                    |                  |
 |                 |--POST /resume-----|                    |                  |
 |                 |                    |--解析 PDF----------|                  |
 |                 |                    |                    |--提取技能--------|
 |                 |                    |<-------------------|                  |
 |                 |                    |-------------------->| 保存简历        |
 |                 |<-------------------|                    |                  |
 |<--返回技能栈----|                    |                    |                  |
 |                 |                    |                    |                  |
 |--开始面试-------|                    |                    |                  |
 |                 |--POST /start------|                    |                  |
 |                 |                    |-------------------->| 创建会话        |
 |                 |<-------------------|                    |                  |
 |<--sessionId-----|                    |                    |                  |
 |                 |                    |                    |                  |
 |--获取问题-------|                    |                    |                  |
 |                 |--GET /question----|                    |                  |
 |                 |-------------------->| 生成问题----------|                  |
 |                 |<-------------------|                    |                  |
 |<--问题----------|                    |                    |                  |
 |                 |                    |                    |                  |
 |--回答问题-------|                    |                    |                  |
 |                 |--POST /answer-----|                    |                  |
 |                 |                    |-------------------->| 保存答案        |
 |                 |                    |--RAG 检索----------|                  |
 |                 |                    |--调用 AI-----------|                  |
 |                 |                    |<-------------------| 流式返回         |
 |                 |<--SSE 流式---------|                    |                  |
 |<--AI 评估-------|                    |                    |                  |
 |                 |                    |                    |                  |
 |  (多轮问答...)  |                    |                    |                  |
 |                 |                    |                    |                  |
 |--结束面试-------|                    |                    |                  |
 |                 |--GET /result------|                    |                  |
 |                 |                    |--计算分数----------|                  |
 |                 |-------------------->| 生成评估----------|                  |
 |                 |<-------------------|                    |                  |
 |<--面试报告------|                    |                    |                  |
```

---

## 七、常见问题与优化建议

### Q1: 如何判断何时结束对当前问题的追问？
**当前实现**: AI Agent 根据回答质量自主判断
**优化建议**: 
- 设置最大追问次数（如 3 次）
- 添加显式的"下一题"指令识别
- 根据面试总时长动态调整

### Q2: 如何处理用户长时间不回答？
**建议方案**:
- 前端添加计时器，超过 5 分钟自动提示
- 提供"跳过"按钮
- 记录超时情况，影响最终评分

### Q3: 如何保证面试的公平性？
**当前实现**:
- 基于岗位和难度标准化题库
- AI 根据统一标准评估
**优化建议**:
- 添加人工复核机制
- 记录评估依据
- 提供申诉通道

### Q4: 如何支持语音面试？
**建议方案**:
- 集成语音识别 (如 Azure Speech、讯飞)
- 前端采集语音并转文字
- 文字传入现有接口
- AI 返回文字，前端 TTS 播放

---

## 八、扩展功能建议

### 1. 面试中暂停/继续
```
新增接口：
- POST /api/interview/pause?sessionId={sessionId}  // 暂停面试
- POST /api/interview/resume?sessionId={sessionId} // 继续面试
```

### 2. 面试记录回放
```
新增接口：
- GET /api/interview/history?userId={userId}  // 获取面试历史
- GET /api/interview/detail/{sessionId}       // 获取面试详情（含完整对话）
```

### 3. 实时提示/求助
```
新增接口：
- POST /api/interview/hint?sessionId={sessionId}&questionId={id}
  返回：当前问题的提示信息
```

### 4. 多轮对话状态机
```
定义明确的面试状态：
INTRODUCTION -> TECHNICAL_1 -> TECHNICAL_2 -> 
SCENARIO_1 -> BEHAVIORAL_1 -> CLOSING -> EVALUATION
```

---

## 九、总结

本系统通过五个核心接口的有机配合，实现了完整的技术面试流程：

1. **简历上传** → 了解候选人背景
2. **开始面试** → 创建面试会话
3. **获取问题** → 动态生成面试问题
4. **提交答案** → AI 实时评估和追问
5. **获取结果** → 生成综合评估报告

**核心优势**:
- ✅ 个性化面试（基于简历 RAG）
- ✅ 实时交互（流式 SSE 输出）
- ✅ 智能追问（AI 动态决策）
- ✅ 完整记录（持久化存储）

**下一步优化方向**:
- 完善 AI 追问逻辑（明确何时切换话题）
- 添加面试进度可视化
- 支持语音输入输出
- 增加人工复核机制

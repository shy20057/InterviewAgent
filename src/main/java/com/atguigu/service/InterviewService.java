package com.atguigu.service;

import com.atguigu.assistant.InterviewAgent;
import com.atguigu.retriever.EmbeddingStoreProvider;
import com.atguigu.entity.InterviewQuestion;
import com.atguigu.entity.InterviewSession;
import com.atguigu.entity.UserResume;
import com.atguigu.entity.vo.*;
import com.atguigu.mapper.InterviewQuestionMapper;
import com.atguigu.mapper.InterviewSessionMapper;
import com.atguigu.mapper.UserResumeMapper;
import com.atguigu.retriever.InterviewContentRetriever;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 面试服务层 - 处理面试会话管理、题目生成、答案评估等核心业务逻辑
 */
@Slf4j
@Service
public class InterviewService {

    @Autowired
    private InterviewAgent interviewAgent;

    @Autowired
    private InterviewContentRetriever contentRetriever;

    @Autowired
    private InterviewSessionMapper sessionMapper;

    @Autowired
    private InterviewQuestionMapper questionMapper;

    @Autowired
    private UserResumeMapper resumeMapper;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStoreProvider embeddingStoreProvider;

    private static final String RESUME_STORAGE_DIR = "uploads/resumes/";

    /**
     * 开始面试 - 创建面试会话并持久化
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO startInterview(StartInterviewRequest request) {
        String sessionId = UUID.randomUUID().toString();

        // 创建面试会话实体
        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(request.getUserId() != null ? request.getUserId() : "default");
        session.setPosition(request.getPosition() != null ? request.getPosition() : "java-backend");
        session.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : "medium");
        session.setStatus("ongoing");
        session.setTotalQuestions(0);
        session.setAnsweredCount(0);
        session.setScore(0.0);
        session.setStartTime(LocalDateTime.now());

        // 持久化到数据库
        sessionMapper.insert(session);
        log.info("面试会话已创建: sessionId={}, position={}, difficulty={}",
                sessionId, session.getPosition(), session.getDifficulty());

        return InterviewSessionVO.builder()
                .sessionId(sessionId)
                .userId(session.getUserId())
                .position(session.getPosition())
                .difficulty(session.getDifficulty())
                .status("ongoing")
                .startTime(session.getStartTime().toString())
                .message("面试已开始，请准备好回答问题！😊")
                .build();
    }

    /**
     * 获取面试问题
     */
    public QuestionVO getQuestion(String sessionId) {
        InterviewSession session = getSession(sessionId);
        if (session == null) {
            return QuestionVO.builder()
                    .sessionId(sessionId)
                    .question("会话不存在，请先开始面试")
                    .build();
        }

        int nextQuestionNumber = session.getTotalQuestions() + 1;

        // 如果是第一个问题，生成自我介绍引导语
        String question;
        if (nextQuestionNumber == 1) {
            question = "你好！我是你的面试官，请简单介绍一下你自己，并说说你对"
                    + getPositionName(session.getPosition()) + "岗位的理解？😊";
        } else {
            question = "请继续回答下一个问题（由AI面试官根据你的表现动态生成）";
        }

        return QuestionVO.builder()
                .sessionId(sessionId)
                .questionNumber(nextQuestionNumber)
                .question(question)
                .category(session.getPosition())
                .difficulty(session.getDifficulty())
                .build();
    }

    /**
     * 提交答案并获取AI反馈（流式）
     */
    @Transactional(rollbackFor = Exception.class)
    public Flux<String> submitAnswer(SubmitAnswerRequest request) {
        InterviewSession session = getSession(request.getSessionId());
        if (session == null) {
            return Flux.just("会话不存在，请先调用 /api/interview/start 开始面试");
        }

        String position = session.getPosition();
        String difficulty = session.getDifficulty();
        String userSkills = session.getUserId(); // 后续可从简历中提取

        // ================= 显式 (Manual) RAG 检索 =====================
        // 这一步代替了原来的 ThreadLocal 上下文传递，彻底解决了 WebFlux 异步切线程导致的空指针。
        java.util.List<dev.langchain4j.rag.content.Content> retrievedContentsList = 
                contentRetriever.retrieveForInterview(request.getAnswer(), position, difficulty, session.getUserId());
        
        String retrievedContents = retrievedContentsList.stream()
                .map(dev.langchain4j.rag.content.Content::textSegment)
                .map(dev.langchain4j.data.segment.TextSegment::text)
                .collect(java.util.stream.Collectors.joining("\n---\n"));
        // ==============================================================

        // 记录用户答案到数据库
        int questionOrder = session.getAnsweredCount() + 1;
        InterviewQuestion questionRecord = new InterviewQuestion();
        questionRecord.setSessionId(request.getSessionId());
        questionRecord.setQuestionOrder(questionOrder);
        questionRecord.setUserAnswer(request.getAnswer());
        questionRecord.setAnswerTime(LocalDateTime.now());
        questionRecord.setDifficulty(difficulty);
        questionMapper.insert(questionRecord);

        // 更新会话的已答题数
        session.setAnsweredCount(questionOrder);
        session.setTotalQuestions(Math.max(session.getTotalQuestions(), questionOrder));
        sessionMapper.updateById(session);

        // 获取当前问题描述
        String currentQuestion = "第" + questionOrder + "题";

        // 调用AI面试Agent (携带检索到的内容)
        return interviewAgent.interview(
                request.getSessionId(),
                request.getAnswer(),
                position,
                difficulty,
                currentQuestion,
                userSkills != null ? userSkills : "Java, Spring, MySQL",
                retrievedContents
        );
    }

    /**
     * 上传并解析简历
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO uploadResume(MultipartFile file, String userId) throws Exception {
        // 1. 保存文件到本地
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path storagePath = Paths.get(RESUME_STORAGE_DIR);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
        Path filePath = storagePath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // 2. 解析 PDF 文本
        Document document = FileSystemDocumentLoader.loadDocument(filePath.toString(), new ApachePdfBoxDocumentParser());
        String fullText = document.text();

        // 3. 利用 AI 提取技能和年限
        String aiJson = interviewAgent.extractSkills(fullText);
        String skills = "未识别到核心技能";
        Integer experienceYears = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(aiJson);
            skills = node.path("skills").asText(skills);
            experienceYears = node.path("experienceYears").asInt(0);
        } catch (Exception e) {
            log.warn("AI 提取技能 JSON 解析失败: {}", aiJson);
        }

        // 4. 将简历向量化并存入 Pinecone (用户隔离 Namespace)
        String namespace = "user-resume-" + userId;
        EmbeddingStore<TextSegment> resumeStore = embeddingStoreProvider.getStoreByNamespace(namespace);
        
        DocumentSplitter splitter = new DocumentByParagraphSplitter(300, 30);
        List<TextSegment> segments = splitter.split(document);
        for (TextSegment segment : segments) {
            segment.metadata().put("userId", userId);
            segment.metadata().put("source", file.getOriginalFilename());
            segment.metadata().put("type", "user-resume");
            
            Response<Embedding> response = embeddingModel.embed(segment);
            resumeStore.add(response.content(), segment);
        }

        // 5. 保存到数据库
        UserResume resume = new UserResume();
        resume.setUserId(userId);
        resume.setResumePath(filePath.toString());
        resume.setSkills(skills);
        resume.setExperienceYears(experienceYears);
        resume.setUploadTime(LocalDateTime.now());
        resumeMapper.insert(resume);

        return ResumeVO.builder()
                .id(resume.getId())
                .userId(userId)
                .resumePath(resume.getResumePath())
                .skills(skills)
                .experienceYears(experienceYears)
                .build();
    }

    /**
     * 获取面试评估结果
     */
    public InterviewResultVO getResult(String sessionId) {
        InterviewSession session = getSession(sessionId);
        if (session == null) {
            return InterviewResultVO.builder()
                    .sessionId(sessionId)
                    .evaluation("会话不存在")
                    .build();
        }

        // 查询该会话的所有答题记录
        LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestion::getSessionId, sessionId);
        List<InterviewQuestion> questions = questionMapper.selectList(wrapper);

        // 计算总分
        double totalScore = questions.stream()
                .filter(q -> q.getScore() != null)
                .mapToInt(InterviewQuestion::getScore)
                .average()
                .orElse(0.0);

        // 更新会话状态
        session.setStatus("completed");
        session.setScore(totalScore);
        session.setEndTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        return InterviewResultVO.builder()
                .sessionId(sessionId)
                .score(totalScore)
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(session.getAnsweredCount())
                .evaluation(generateEvaluation(totalScore, questions.size()))
                .endTime(session.getEndTime().toString())
                .build();
    }

    /**
     * 根据sessionId获取会话
     */
    private InterviewSession getSession(String sessionId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getSessionId, sessionId);
        return sessionMapper.selectOne(wrapper);
    }

    /**
     * 获取岗位中文名称
     */
    private String getPositionName(String position) {
        switch (position) {
            case "java-backend": return "Java后端开发";
            case "frontend": return "前端开发";
            case "fullstack": return "全栈开发";
            default: return position;
        }
    }

    /**
     * 根据分数生成评估文本
     */
    private String generateEvaluation(double score, int questionCount) {
        if (questionCount == 0) {
            return "您还没有回答任何问题，无法给出评估。";
        }
        if (score >= 90) {
            return "表现优秀！基础知识扎实，思路清晰，建议继续深化高级话题。🌟";
        } else if (score >= 75) {
            return "表现良好！基础知识掌握较好，建议加强系统设计和场景分析能力。💪";
        } else if (score >= 60) {
            return "表现一般，部分知识点需要加强。建议多做练习，查漏补缺。📚";
        } else {
            return "需要加强学习，基础知识还有较大提升空间。建议系统化学习后再来挑战。📖";
        }
    }
}

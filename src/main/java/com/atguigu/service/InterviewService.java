package com.atguigu.service;

import com.atguigu.assistant.EvaluationAgent;
import com.atguigu.assistant.InterviewAgent;
import com.atguigu.entity.dto.StartInterviewDTO;
import com.atguigu.entity.dto.SubmitAnswerRequestDTO;
import com.atguigu.entity.po.InterviewQuestion;
import com.atguigu.entity.po.InterviewSession;
import com.atguigu.entity.po.UserResume;
import com.atguigu.entity.vo.InterviewSessionVO;
import com.atguigu.entity.vo.QuestionVO;
import com.atguigu.entity.vo.ResumeVO;
import com.atguigu.mapper.InterviewQuestionMapper;
import com.atguigu.mapper.InterviewSessionMapper;
import com.atguigu.mapper.UserResumeMapper;
import com.atguigu.retriever.EmbeddingStoreProvider;
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
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 面试服务层 - 处理面试会话管理、题目生成、答案评估等核心业务逻辑
 */
@Slf4j
@Service
public class InterviewService {

    @Autowired
    private InterviewAgent interviewAgent;

    @Autowired
    private EvaluationAgent evaluationAgent;

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

    @Value("${resume.storage.path:D:/.interview_resumes}")
    private String resumeStoragePath;

    /**
     * 开始面试 - 创建面试会话并持久化
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO startInterview(StartInterviewDTO startInterviewDTO) {
        String sessionId = UUID.randomUUID().toString();

        // 创建面试会话实体
        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(startInterviewDTO.getUserId() != null ? startInterviewDTO.getUserId() : "default");
        session.setPosition(startInterviewDTO.getPosition() != null ? startInterviewDTO.getPosition() : "java-backend");
        session.setDifficulty(startInterviewDTO.getDifficulty() != null ? startInterviewDTO.getDifficulty() : "medium");
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
                .message("面试已开始，请准备好回答问题！")
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
            question = "你好！我是你的面试官，请简单介绍一下你自己。";
        } else {
            // TODO 查询知识库提问
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
    public Flux<String> submitAnswer(SubmitAnswerRequestDTO submitAnswerRequestDTO) {
        InterviewSession session = getSession(submitAnswerRequestDTO.getSessionId());
        if (session == null) {
            return Flux.just("会话不存在，请先调用 /api/interview/start 开始面试");
        }

        String position = session.getPosition();
        String difficulty = session.getDifficulty();
        String userSkills = session.getUserId();
        // TODO 后续可从简历中提取

        // ================= RAG 检索 ====================
       List<Content> retrievedContentsList = contentRetriever
                .retrieveForInterview(submitAnswerRequestDTO.getAnswer(), position, difficulty, session.getUserId());

        // 提取片段中的纯文本，并进行拼接
        String retrievedContents = retrievedContentsList.stream()
                .map(Content::textSegment)
                .map(TextSegment::text)
                .collect(Collectors.joining("\n---\n"));
        // ==============================================================

        String resumeContent = "项目经历：个性推荐电商";
        // 1. 记录用户答案到数据库
        InterviewQuestion questionRecord = InterviewQuestion.builder()
                .sessionId(submitAnswerRequestDTO.getSessionId())
                .category(session.getPosition())
                .difficulty(session.getDifficulty())
                .userAnswer(submitAnswerRequestDTO.getAnswer())
                .answerTime(LocalDateTime.now())
                .build();
        questionMapper.insert(questionRecord);

        // 2. 调用面试智能体 (负责对话)
        Flux<String> interviewerFlow = interviewAgent.interview(
                submitAnswerRequestDTO.getSessionId(),
                submitAnswerRequestDTO.getAnswer(),
                position,
                difficulty,
                userSkills != null ? userSkills : "Java, Spring, MySQL",
                retrievedContents,
                resumeContent
        );

        // 3. 背景处理：在流结束后静默执行评估
        StringBuilder interviewerSpeech = new StringBuilder();

        return interviewerFlow
                .doOnNext(interviewerSpeech::append)
                .doFinally(signalType -> {
                    if (reactor.core.publisher.SignalType.ON_COMPLETE.equals(signalType)) {
                        // 异步执行评估，不阻塞主流程返回，且前端不可见
                        CompletableFuture.runAsync(() -> {
                            try {
                                log.info("开始后台评估 [Session: {}]", submitAnswerRequestDTO.getSessionId());
                                String evalResult = evaluationAgent.evaluate(
                                        submitAnswerRequestDTO.getSessionId(),
                                        submitAnswerRequestDTO.getAnswer(),
                                        position,
                                        retrievedContents
                                );
                                // 保存完整的面试官回复
                                questionRecord.setAgentQuestion(interviewerSpeech.toString());
                                // 解析并保存评估得分
                                extractAndSaveScores(evalResult, questionRecord.getId());
                            } catch (Exception e) {
                                log.error("后台评估失败: {}", e.getMessage());
                            }
                        });
                    }
                });
    }

    /**
     * 解析评估 JSON 并更新数据库记录
     */
    private void extractAndSaveScores(String jsonText, Long recordId) {
        try {
            InterviewQuestion record = questionMapper.selectById(recordId);
            if (record == null) return;

            // 尝试从字符串中解析 JSON (可能包含在 ```json 中)
            String jsonStr = jsonText;
            if (jsonText.contains("```json")) {
                Pattern pattern = Pattern.compile("```json\\s*(\\{[\\s\\S]*?\\})\\s*```");
                Matcher matcher = pattern.matcher(jsonText);
                if (matcher.find()) {
                    jsonStr = matcher.group(1);
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonStr);
            JsonNode tech = rootNode.path("evaluation").path("technical");
            
            if (!tech.isMissingNode()) {
                record.setTechAccuracy(tech.path("techAccuracy").asDouble(0.0));
                record.setTechDepth(tech.path("techDepth").asDouble(0.0));
                record.setLogicCohesion(tech.path("logicCohesion").asDouble(0.0));
                record.setJobMatchScore(tech.path("jobMatchScore").asDouble(0.0));
                
                log.info("成功提取后台评分 [ID:{}]: Accuracy={}, Depth={}", 
                        recordId, record.getTechAccuracy(), record.getTechDepth());
            }

            questionMapper.updateById(record);

            // 更新 Session 进度
            InterviewSession session = sessionMapper.selectOne(new LambdaQueryWrapper<InterviewSession>()
                    .eq(InterviewSession::getSessionId, record.getSessionId()));
            if (session != null) {
                session.setAnsweredCount(session.getAnsweredCount() + 1);
                sessionMapper.updateById(session);
            }
        } catch (Exception e) {
            log.error("解析后台评估 JSON 失败 [ID:{}]: {}", recordId, e.getMessage());
        }
    }

    /**
     * 上传并解析简历
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO uploadResume(MultipartFile file, String userId) throws Exception {
        // 1. 保存文件到本地
        String fileName = "UID-" + userId + "-" + file.getOriginalFilename();

        Path storagePath = Paths.get(resumeStoragePath);

        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
        Path filePath = storagePath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // 2. 根据文件类型解析文本
        String fullText = "";
        Document document = null;

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            if (originalFilename.endsWith(".pdf")) {
                // 解析 PDF 文本
                document = FileSystemDocumentLoader.loadDocument(filePath.toString(), new ApachePdfBoxDocumentParser());
                fullText = document.text();
            } else if (originalFilename.endsWith(".docx")) {
                // 解析 Word 文档
                fullText = parseWordDocument(filePath.toFile());
                // 创建文档对象用于后续处理
                document = Document.from(fullText);
            } else if (originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg")
                    || originalFilename.endsWith(".png")) {
                System.out.println("图片ocr待做");
            } else {
                throw new IllegalArgumentException("不支持的文件格式，请上传 PDF、Word 文档或图片文件");
            }
        } else {
            throw new IllegalArgumentException("文件名不能为空");
        }

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
     * 解析 Word 文档
     */
    private String parseWordDocument(File file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file.toPath()));
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }


//    /**
//     * 获取面试评估结果
//     */
//    public InterviewResultVO getResult(String sessionId) {
//        InterviewSession session = sessionMapper.selectById(sessionId);
//        if (session == null) {
//            return InterviewResultVO.builder()
//                    .sessionId(sessionId)
//                    .evaluation("会话不存在")
//                    .build();
//        }
//
//        // 查询该会话的所有答题记录
//        LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(InterviewQuestion::getSessionId, sessionId);
//        List<InterviewQuestion> questions = questionMapper.selectList(wrapper);
//
//        // 计算总分
//        double totalScore = questions.stream()
//                .filter(q -> q.getScore() != null)
//                .mapToInt(InterviewQuestion::getScore)
//                .average()
//                .orElse(0.0);
//
//        // 更新会话状态
//        session.setStatus("completed");
//        session.setScore(totalScore);
//        session.setEndTime(LocalDateTime.now());
//        sessionMapper.updateById(session);
//
//        return InterviewResultVO.builder()
//                .sessionId(sessionId)
//                .score(totalScore)
//                .totalQuestions(session.getTotalQuestions())
//                .answeredQuestions(session.getAnsweredCount())
//                .evaluation(generateEvaluation(totalScore, questions.size()))
//                .endTime(session.getEndTime().toString())
//                .build();
//    }

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
            case "java-backend":
                return "Java后端开发";
            case "frontend":
                return "前端开发";
            case "fullstack":
                return "全栈开发";
            default:
                return position;
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

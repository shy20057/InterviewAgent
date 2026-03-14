package com.atguigu.service;

import com.atguigu.assistant.ExtractSkillsAgent;
import com.atguigu.assistant.InterviewAgent;
import com.atguigu.entity.dto.StartInterviewDTO;
import com.atguigu.entity.dto.SubmitAnswerRequestDTO;
import com.atguigu.entity.po.InterviewQuestion;
import com.atguigu.entity.po.InterviewSession;
import com.atguigu.entity.po.UserResume;
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
import reactor.core.publisher.SignalType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    private ExtractSkillsAgent extractSkillsAgent;


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
    public Flux<String> startInterview(StartInterviewDTO startInterviewDTO) {
        if(startInterviewDTO.getUserId() == null){
            return Flux.error(new RuntimeException("请先注册"));
        }

        String sessionId = UUID.randomUUID().toString();

        UserResume userResume = resumeMapper.selectOne(new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, startInterviewDTO.getUserId()));

        String position = userResume.getPosition();
        List<String> skillsList = Arrays.asList(userResume.getSkills().split(","));
        String projectExperience = userResume.getProjectExperience();

        if(userResume == null){
            position = startInterviewDTO.getPosition();
            skillsList = Arrays.asList(startInterviewDTO.getUserSkills().split(","));
            projectExperience = "";
        }

        // 创建面试会话实体
        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(userResume.getUserId());
        session.setPosition(position);
        session.setDifficulty(startInterviewDTO.getDifficulty());
        session.setSkills(skillsList.toString());
        session.setStatus("ongoing");
        session.setTotalQuestions(0);
        session.setAnsweredCount(0);
        session.setScore(0.0);
        session.setStartTime(LocalDateTime.now());

        // 持久化到数据库
        sessionMapper.insertOrUpdate(session);
        log.info("面试会话已创建: sessionId={}, position={}, difficulty={}",
                sessionId, session.getPosition(), session.getDifficulty());

        // 根据用户岗位 技术栈检索知识库 获取题库
        List<Content> retrievedContentsList = contentRetriever
                .retrieve4InitQuestion(position, skillsList, projectExperience, startInterviewDTO.getDifficulty(), session.getUserId());

        String retrievedContents = retrievedContentsList.stream()
                .map(Content::textSegment)
                .map(TextSegment::text)
                .collect(Collectors.joining("\n---\n"));


        return interviewAgent.interviewInit(
                sessionId,
                "你好",
                position,
                skillsList.stream().map(skill -> "\"" + skill + "\"").collect(Collectors.joining(",")),
                projectExperience,
                startInterviewDTO.getDifficulty(),
                retrievedContents
        );
    }



    /**
     * 提交答案并获取AI反馈提问
     */
    @Transactional(rollbackFor = Exception.class)
    public Flux<String> submitAnswer(SubmitAnswerRequestDTO submitAnswerRequestDTO) {
        InterviewSession session = getSession(submitAnswerRequestDTO.getSessionId());
        if (session == null) {
            return Flux.just("会话不存在，请先调用 /api/interview/start 开始面试");
        }

        List<String> skillsList = Arrays.asList(session.getSkills().split(","));
        // ================= RAG 检索 ====================
        // 根据用户的回答检索题库 和 简历
       List<Content> retrievedContentsList = contentRetriever
                .retrieve4Answer(submitAnswerRequestDTO.getAnswer(),session.getPosition(),skillsList,session.getDifficulty(),session.getUserId());

        // 提取片段中的纯文本，并进行拼接
        String retrievedContents = retrievedContentsList.stream()
                .map(Content::textSegment)
                .map(TextSegment::text)
                .collect(Collectors.joining("\n---\n"));
        // ==============================================================


        // 1. 记录用户答案到数据库
        InterviewQuestion questionRecord = InterviewQuestion.builder()
                .sessionId(submitAnswerRequestDTO.getSessionId())
                .category(session.getSkills())
                .userAnswer(submitAnswerRequestDTO.getAnswer())
                .answerTime(LocalDateTime.now())
                .build();
        questionMapper.insert(questionRecord);

        // 2. 调用面试智能体 (负责对话)
        Flux<String> interviewerFlow = interviewAgent.interview(
             submitAnswerRequestDTO.getSessionId(),
             submitAnswerRequestDTO.getAnswer(),
             retrievedContents
        );

        // 3. 背景处理：在流结束后静默执行评估
        StringBuilder interviewerSpeech = new StringBuilder();

        return interviewerFlow
                .doOnNext(interviewerSpeech::append)
                .doFinally(signalType -> {
                    if (SignalType.ON_COMPLETE.equals(signalType)) {
                        // 异步执行评估，不阻塞主流程返回，且前端不可见
                        CompletableFuture.runAsync(() -> {
                            try {
                                log.info("开始后台评估 [Session: {}]", submitAnswerRequestDTO.getSessionId());
                                String evalResult = interviewAgent.evaluate(
                                        submitAnswerRequestDTO.getSessionId(),
                                        submitAnswerRequestDTO.getAnswer(),
                                        retrievedContents
                                );

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
        file.transferTo(filePath.toFile()); // 将文件file复制到filePath.toFile()中去

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
                // TODO 图片处理
                System.out.println("图片ocr待做");
            } else {
                throw new IllegalArgumentException("不支持的文件格式，请上传 PDF、Word 文档或图片文件");
            }
        } else {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 3. 利用 AI 提取技能和年限
        String aiJson = extractSkillsAgent.extractSkills(fullText);

        // ==================== JSON 解析和信息提取 ====================
        String skills = "未识别到核心技能";
        String position = "official-java-backend";
        String projectExperience = "暂无相关项目经验";

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 步骤 1：将 JSON 字符串解析为 JsonNode 对象
            // aiJson 示例：{"skills": "java-basic, springboot, mysql, redis", "position": "official-java-backend", "projectExperience": "..."}
            JsonNode jsonNode = mapper.readTree(aiJson);

            // 步骤 2：分别提取三个字段的信息

            // ① 提取 skills（技术栈）
            if (jsonNode.has("skills") && !jsonNode.get("skills").isMissingNode()) {
                skills = jsonNode.get("skills").asText();
                // skills = "java-basic, springboot, mysql, redis"
            }

            // ② 提取 position（岗位）
            if (jsonNode.has("position") && !jsonNode.get("position").isMissingNode()) {
                position = jsonNode.get("position").asText();
                // position = "official-java-backend"
            }

            // ③ 提取 projectExperience（项目经历）
            if (jsonNode.has("projectExperience") && !jsonNode.get("projectExperience").isMissingNode()) {
                projectExperience = jsonNode.get("projectExperience").asText();
                // projectExperience = "3 年电商系统开发经验，负责后端架构设计和性能优化..."
            }

            log.info("✅ AI 简历解析成功:");
            log.info("   - 技能栈：{}", skills);
            log.info("   - 目标岗位：{}", position);
            log.info("   - 项目经历：{}", projectExperience);

        } catch (Exception e) {
            log.error("AI 提取 JSON 解析失败: {}", e.getMessage());
            log.warn("原始 AI 返回数据：{}", aiJson);
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
        resume.setProjectExperience(projectExperience);
        resume.setPosition(position);
        resume.setUploadTime(LocalDateTime.now());
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        return ResumeVO.builder()
                .id(resume.getId())
                .userId(userId)
                .resumePath(resume.getResumePath())
                .skills(skills)
                .projectExperience(projectExperience)
                .position(position)
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

}

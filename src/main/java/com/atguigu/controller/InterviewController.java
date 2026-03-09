package com.atguigu.controller;

import com.atguigu.assistant.InterviewAgent;
import com.atguigu.entity.InterviewSession;
import com.atguigu.entity.InterviewQuestion;
import com.atguigu.retriever.InterviewContentRetriever;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "面试模拟")
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Autowired
    private InterviewAgent interviewAgent;

    @Autowired
    private InterviewContentRetriever contentRetriever;

    @Operation(summary = "开始面试")
    @PostMapping("/start")
    public Map<String, Object> startInterview(@RequestBody Map<String, String> request) {
        String userId = request.getOrDefault("userId", "default");
        String position = request.getOrDefault("position", "java-backend");
        String difficulty = request.getOrDefault("difficulty", "medium");
        String userSkills = request.getOrDefault("userSkills", "");

        String sessionId = UUID.randomUUID().toString();

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("userId", userId);
        response.put("position", position);
        response.put("difficulty", difficulty);
        response.put("startTime", LocalDateTime.now().toString());
        response.put("message", "面试已开始，请准备好回答问题！😊");

        return response;
    }

    @Operation(summary = "获取面试问题")
    @GetMapping("/question")
    public Map<String, Object> getQuestion(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("question", "你好！我是你的面试官，请简单介绍一下你自己，并说说你对" + 
            getSessionPosition(sessionId) + "岗位的理解？😊");
        response.put("questionNumber", 1);
        return response;
    }

    @Operation(summary = "提交答案")
    @PostMapping("/answer")
    public Flux<String> submitAnswer(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String userAnswer = request.get("answer");
        String position = getSessionPosition(sessionId);
        String difficulty = getSessionDifficulty(sessionId);
        String currentQuestion = getCurrentQuestion(sessionId);
        String userSkills = getSessionSkills(sessionId);

        return interviewAgent.interview(
                sessionId,
                userAnswer,
                position,
                difficulty,
                currentQuestion,
                userSkills
        );
    }

    @Operation(summary = "获取面试评估")
    @GetMapping("/evaluation")
    public Map<String, Object> getEvaluation(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("score", 85);
        response.put("totalQuestions", 5);
        response.put("answeredQuestions", 3);
        response.put("evaluation", "表现良好，基础知识扎实，建议加强系统设计能力。继续加油！💪");
        response.put("endTime", LocalDateTime.now().toString());
        return response;
    }

    private String getSessionPosition(String sessionId) {
        return "java-backend";
    }

    private String getSessionDifficulty(String sessionId) {
        return "medium";
    }

    private String getCurrentQuestion(String sessionId) {
        return "当前问题";
    }

    private String getSessionSkills(String sessionId) {
        return "Java, Spring, MySQL";
    }
}

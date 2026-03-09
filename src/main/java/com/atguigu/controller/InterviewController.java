package com.atguigu.controller;

import com.atguigu.entity.vo.*;
import com.atguigu.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "面试模拟")
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    /**
     * 开始面试
     */
    @Operation(summary = "开始面试")
    @PostMapping("/start")
    public ResponseEntity<InterviewSessionVO> startInterview(@RequestBody StartInterviewRequest request) {
        InterviewSessionVO session = interviewService.startInterview(request);
        return ResponseEntity.ok(session);
    }

    /**
     * 获取面试问题
     */
    @Operation(summary = "获取面试问题")
    @GetMapping("/question")
    public ResponseEntity<QuestionVO> getQuestion(@RequestParam String sessionId) {
        QuestionVO question = interviewService.getQuestion(sessionId);
        return ResponseEntity.ok(question);
    }

    /**
     * 提交答案（流式返回AI反馈）
     */
    @Operation(summary = "提交答案")
    @PostMapping(value = "/answer", produces = "text/event-stream;charset=utf-8")
    public Flux<String> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        return interviewService.submitAnswer(request);
    }

    /**
     * 获取面试评估结果
     */
    @Operation(summary = "获取面试结果")
    @GetMapping("/result/{sessionId}")
    public ResponseEntity<InterviewResultVO> getResult(@PathVariable String sessionId) {
        InterviewResultVO result = interviewService.getResult(sessionId);
        return ResponseEntity.ok(result);
    }

    /**
     * 上传并解析简历
     */
    @Operation(summary = "上传简历")
    @PostMapping("/resume/upload")
    public ResponseEntity<ResumeVO> uploadResume(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("userId") String userId) throws Exception {
        
        ResumeVO resume = interviewService.uploadResume(file, userId);
        return ResponseEntity.ok(resume);
    }
}

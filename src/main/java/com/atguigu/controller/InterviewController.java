package com.atguigu.controller;

<<<<<<< HEAD
import com.atguigu.entity.dto.StartInterviewDTO;
import com.atguigu.entity.dto.SubmitAnswerRequestDTO;
=======
>>>>>>> 937f045ba68541c536ea36d8d25054ac5e48a0c0
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
<<<<<<< HEAD
    public ResponseEntity<InterviewSessionVO> startInterview(@RequestBody StartInterviewDTO startInterviewDTO) {
        InterviewSessionVO session = interviewService.startInterview(startInterviewDTO);
=======
    public ResponseEntity<InterviewSessionVO> startInterview(@RequestBody StartInterviewRequest request) {
        InterviewSessionVO session = interviewService.startInterview(request);
>>>>>>> 937f045ba68541c536ea36d8d25054ac5e48a0c0
        return ResponseEntity.ok(session);
    }

    /**
     * 获取面试问题
     */
    @Operation(summary = "获取面试问题")
    @GetMapping("/question")
<<<<<<< HEAD
    public ResponseEntity<QuestionVO> getQuestion(@RequestParam("sessionId") String sessionId) {
=======
    public ResponseEntity<QuestionVO> getQuestion(@RequestParam String sessionId) {
>>>>>>> 937f045ba68541c536ea36d8d25054ac5e48a0c0
        QuestionVO question = interviewService.getQuestion(sessionId);
        return ResponseEntity.ok(question);
    }

    /**
     * 提交答案（流式返回AI反馈）
     */
    @Operation(summary = "提交答案")
    @PostMapping(value = "/answer", produces = "text/event-stream;charset=utf-8")
<<<<<<< HEAD
    public Flux<String> submitAnswer(@RequestBody SubmitAnswerRequestDTO submitAnswerRequestDTO) {
        return interviewService.submitAnswer(submitAnswerRequestDTO);
=======
    public Flux<String> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        return interviewService.submitAnswer(request);
>>>>>>> 937f045ba68541c536ea36d8d25054ac5e48a0c0
    }

    /**
     * 获取面试评估结果
     */
    @Operation(summary = "获取面试结果")
    @GetMapping("/result/{sessionId}")
<<<<<<< HEAD
    public ResponseEntity<InterviewResultVO> getResult(@PathVariable("sessionId") String sessionId) {
=======
    public ResponseEntity<InterviewResultVO> getResult(@PathVariable String sessionId) {
>>>>>>> 937f045ba68541c536ea36d8d25054ac5e48a0c0
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

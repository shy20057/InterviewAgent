package com.atguigu.controller;

import com.atguigu.entity.dto.StartInterviewDTO;
import com.atguigu.entity.dto.SubmitAnswerRequestDTO;
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
    public Flux<String> startInterview(@RequestBody StartInterviewDTO startInterviewDTO) {
       return interviewService.startInterview(startInterviewDTO);
    }



    /**
     * 面试对话
     */
    @Operation(summary = "面试对话")
    @PostMapping(value = "/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> chat(@RequestBody SubmitAnswerRequestDTO submitAnswerRequestDTO) {
        return interviewService.submitAnswer(submitAnswerRequestDTO);
    }

//    /**
//     * 获取面试评估结果
//     */
//    @Operation(summary = "获取面试结果")
//    @GetMapping("/result/{sessionId}")
//    public ResponseEntity<InterviewResultVO> getResult(@PathVariable("sessionId") String sessionId) {
//        InterviewResultVO result = interviewService.getResult(sessionId);
//        return ResponseEntity.ok(result);
//    }

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

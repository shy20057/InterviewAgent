package com.atguigu.controller;

import com.atguigu.assistant.InterviewAgent;
import com.atguigu.entity.po.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "AI面试官")
@RestController
@RequestMapping("/api/interview")
public class XiaozhiController {

    @Autowired
    private InterviewAgent interviewAgent;

    @Operation(summary = "面试对话")
    @PostMapping(value="/answer", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm){
        return interviewAgent.interview(
                chatForm.getMemoryId(),
                chatForm.getMessage(),
                "java-backend",
                "medium",
                "当前问题",
                "Java, Spring, MySQL"
        );
    }
}

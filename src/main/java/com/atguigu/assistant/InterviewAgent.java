package com.atguigu.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "interviewChatMemoryProvider"
)
public interface InterviewAgent {

    @SystemMessage(fromResource = "interviewInit_prompt_template.txt")
    Flux<String> interviewInit(
            @MemoryId String sessionId,
            @UserMessage String userMessage, //用户的实际消息/问题/回答
            @V("position") String position, //面试职位（如：Java 开发、前端工程师）
            @V("skills") String skills,
            @V("project_experience") String projectExperience,
            @V("difficulty") String difficulty,
            @V("retrieved_contents") String retrievedContents
    );

    @UserMessage(fromResource = "interview_prompt_template.txt")
    Flux<String> interview(
            @MemoryId String sessionId,
            @V("user_message") String userMessage,
            @V("retrieved_contents") String retrievedContents
    );

    @UserMessage(fromResource = "evaluate_prompt_template.txt")
    String evaluate(
            @MemoryId String sessionId,
            @V("user_answer") String userAnswer,
            @V("retrieved_contents") String retrievedContents
    );


}

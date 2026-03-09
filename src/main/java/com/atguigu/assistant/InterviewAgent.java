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
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "interviewChatMemoryProvider",
        contentRetriever = "interviewContentRetriever"
)
public interface InterviewAgent {

    @SystemMessage(fromResource = "interview_prompt_template.txt")
    Flux<String> interview(
            @MemoryId String sessionId,
            @UserMessage String userMessage,
            @V("position") String position,
            @V("difficulty") String difficulty,
            @V("current_question") String currentQuestion,
            @V("user_skills") String userSkills
    );
}

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
        chatMemoryProvider = "interviewChatMemoryProvider"
)
public interface InterviewAgent {

    @SystemMessage(fromResource = "interview_prompt_template.txt")
    Flux<String> interview(
            @MemoryId String sessionId,
            @UserMessage String userMessage,
            @V("position") String position,
            @V("difficulty") String difficulty,
            @V("current_question") String currentQuestion,
            @V("user_skills") String userSkills,
            @V("retrieved_contents") String retrievedContents
    );

    @SystemMessage("你是一个专业的简历分析师。请从提供的简历文本中提取出技术栈（技能）和工作年限。以 JSON 格式返回，例如：{\"skills\": \"Java, Spring, MySQL\", \"experienceYears\": 3}")
    String extractSkills(@UserMessage String resumeText);
}

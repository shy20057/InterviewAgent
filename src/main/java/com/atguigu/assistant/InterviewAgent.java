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
<<<<<<< HEAD
            @UserMessage String userMessage, //用户的实际消息/问题/回答
            @V("position") String position, //面试职位（如：Java 开发、前端工程师）
            @V("difficulty") String difficulty, //面试难度（如：简单、中等、困难）
            @V("current_question") String currentQuestion,//当前问题
            @V("user_skills") String userSkills,//用户技能栈（从简历提取的技术栈）
            @V("retrieved_contents") String retrievedContents //检索到的相关内容（从知识库 RAG 检索的参考资料）
=======
            @UserMessage String userMessage,
            @V("position") String position,
            @V("difficulty") String difficulty,
            @V("current_question") String currentQuestion,
            @V("user_skills") String userSkills,
            @V("retrieved_contents") String retrievedContents
>>>>>>> 937f045ba68541c536ea36d8d25054ac5e48a0c0
    );

    @SystemMessage("你是一个专业的简历分析师。请从提供的简历文本中提取出技术栈（技能）和工作年限。以 JSON 格式返回，例如：{\"skills\": \"Java, Spring, MySQL\", \"experienceYears\": 3}")
    String extractSkills(@UserMessage String resumeText);
}

package com.atguigu.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 评估智能体 - 后台静默执行，负责对面试回答进行打分和 JSON 数据封装
 * 该方法为同步阻塞调用，由 Service 层通过异步任务触发
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "interviewChatMemoryProvider"
)
public interface EvaluationAgent {

    @SystemMessage("你负责评估用户回答")
    String evaluate(
            @MemoryId String sessionId,
            @UserMessage String userAnswer,
            @V("position") String position,
            @V("retrieved_contents") String retrievedContents
    );
}

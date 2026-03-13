//package com.atguigu.assistant;
//
//import dev.langchain4j.data.message.UserMessage;
//import dev.langchain4j.service.spring.AiService;
//
//import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;
//
//@AiService(
//        wiringMode = EXPLICIT,
//        chatModel = "chatVisionModel"
//)
//public interface MultiModalAgent {
//    /**
//     * 识别文件（图片）中的内容
//     * @param userMessage 包含文本提示和图片内容的混合消息
//     * @return AI 的分析结果
//     */
//    String chat(UserMessage userMessage);
//}

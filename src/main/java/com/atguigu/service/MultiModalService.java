//package com.atguigu.service;
//
//import com.atguigu.assistant.MultiModalAgent;
//import dev.langchain4j.data.image.Image;
//import dev.langchain4j.data.message.ImageContent;
//import dev.langchain4j.data.message.TextContent;
//import dev.langchain4j.data.message.UserMessage;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//
//@Slf4j
//@Service
//public class MultiModalService {
//
//    @Autowired
//    private MultiModalAgent multiModalAgent;
//
//    public String recognizeFile(String imageUrl, String prompt) throws IOException {
//        log.info("开始识别图片，URL: {}, Prompt: {}", imageUrl, prompt);
//
//        // 创建 Image 对象，指定 URL
//        Image image = Image.builder()
//                .url(imageUrl)
//                .build();
//
//        // 封装为 ImageContent
//        ImageContent imageContent = ImageContent.from(image);
//        log.info("ImageContent 创建成功：{}", imageContent);
//
//        // 结合用户的 Prompt
//        TextContent textContent = TextContent.from(prompt);
//
//        // 封装为 UserMessage 并发送给智能体
//        UserMessage message = UserMessage.from(textContent, imageContent);
//        log.info("UserMessage 创建成功，包含 {} 个内容元素", message.contents().size());
//
//        String result = multiModalAgent.chat(message);
//        log.info("AI 返回结果：{}", result);
//
//        return result;
//    }
//}

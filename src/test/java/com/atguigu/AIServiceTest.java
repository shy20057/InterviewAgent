//package com.atguigu;
//
//import com.alibaba.dashscope.assistants.Assistant;
//import dev.langchain4j.community.model.dashscope.QwenChatModel;
//import dev.langchain4j.service.AiServices;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//public class AIServiceTest {
//
//    @Autowired
//    private QwenChatModel qwenChatModel;
//
//    @Test
//    public void testChat(){
//        // 获取Assistant对象
//        Assistant assistant = AiServices.create(Assistant.class, qwenChatModel);
//        String answer = assistant.chat("你是谁");
//        System.out.println(answer);
//    }
//
//    @Autowired
//    private Assistant assistant;
//    @Test
//    public void testChat2(){
//        String answer = assistant.chat("你是谁");
//        System.out.println(answer);
//    }
//}

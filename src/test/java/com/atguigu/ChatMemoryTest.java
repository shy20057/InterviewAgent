package com.atguigu;

import com.atguigu.assistant.MemoryChatAssistant;
import com.atguigu.assistant.SeparateChatAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class  ChatMemoryTest {

    @Autowired
    private QwenChatModel qwenChatModel;
    @Test
    public void testChatMemory2() {
//第一轮对话
        UserMessage userMessage1 = UserMessage.userMessage("我是环环");
        ChatResponse chatResponse1 = qwenChatModel.chat(userMessage1);
        AiMessage aiMessage1 = chatResponse1.aiMessage();
//输出大语言模型的回复
        System.out.println(aiMessage1.text());
//第二轮对话
        UserMessage userMessage2 = UserMessage.userMessage("你知道我是谁吗");
        ChatResponse chatResponse2 = qwenChatModel.chat(Arrays.asList(userMessage1, // 这里是关键点
                aiMessage1, userMessage2));
        AiMessage aiMessage2 = chatResponse2.aiMessage();
//输出大语言模型的回复
        System.out.println(aiMessage2.text());
    }


    @Test
    public void testChatMemory3() {

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10); // 设置缓存的轮次 最大记忆数量

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(qwenChatModel)
                .chatMemory(chatMemory) //这样建立的assistant具有聊天记忆
                .build();

        String answer1 = assistant.chat("我是缓缓");
        System.out.println(answer1);
        String answer2 = assistant.chat("你知道我是谁吗");
        System.out.println(answer2);

    }

    @Autowired
    private MemoryChatAssistant memoryChatAssistant;

    @Test
    public void testChatMemory4() {

        String answer1 = memoryChatAssistant.chat("我是缓缓");
        System.out.println(answer1);
        String answer2 = memoryChatAssistant.chat("你知道我是谁吗");
        System.out.println(answer2);

    }

    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void testChatMemory5() {

//        String answer1 = separateChatAssistant.chat("1","我现在叫张三");
//        System.out.println(answer1);
        String answer2 = separateChatAssistant.chat("1","今天是几号");
        System.out.println(answer2);

//        String answer3 = separateChatAssistant.chat("2","我叫什么");
//        System.out.println(answer3);

    }
}

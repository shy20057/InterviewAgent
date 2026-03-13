package com.atguigu.config;

import com.atguigu.mongodb.MongoChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterviewAgentConfig {

    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;



//    @Bean
//    public ChatLanguageModel modelScopeChatModel() {
//        return OpenAiChatModel.builder()
//                .baseUrl("https://api-inference.modelscope.ai/v1")
//                .apiKey("ms-1cbf64b5-375f-4ce0-8d67-2dbf3e0d1d0b")
//                .modelName("deepseek-ai/DeepSeek-R1-0528")
//                .temperature(0.8)
//                .timeout(Duration.parse("PT120S"))
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//    }
//
//    @Bean
//    public StreamingChatLanguageModel modelScopeStreamingChatModel() {
//        return OpenAiStreamingChatModel.builder()
//                .baseUrl(baseUrl)
//                .apiKey(apiKey)
//                .modelName(chatModelName)
//                .temperature(temperature)
//                .timeout(Duration.parse(timeout))
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//    }


    // 记忆提供商
    @Bean
    public ChatMemoryProvider interviewChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }


    




}
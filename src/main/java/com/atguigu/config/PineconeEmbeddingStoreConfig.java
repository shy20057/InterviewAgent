package com.atguigu.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PineconeEmbeddingStoreConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean("interviewEmbeddingStore")
    public EmbeddingStore<TextSegment> interviewEmbeddingStore() {
        EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(System.getenv("PINECONE_API_KEY"))
                .index("interview-assistant-index")
                .nameSpace("default")
                .createIndex(PineconeServerlessIndexConfig.builder()
                                .cloud("AWS")
                                .region("us-east-1")
                                .dimension(embeddingModel.dimension())
                                .build())
                .build();
        return embeddingStore;
    }

    public EmbeddingStore<TextSegment> getStoreByNamespace(String namespace) {
        return PineconeEmbeddingStore.builder()
                .apiKey(System.getenv("PINECONE_API_KEY"))
                .index("interview-assistant-index")
                .nameSpace(namespace)
                .build();
    }
}
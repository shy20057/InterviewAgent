package com.atguigu.retriever;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pinecone 向量数据库提供者
 */
@Component
@ConditionalOnProperty(name = "vector.store.type", havingValue = "pinecone", matchIfMissing = true)
public class PineconeEmbeddingStoreProvider implements EmbeddingStoreProvider {

    @Override
    public EmbeddingStore<TextSegment> getStoreByNamespace(String namespace) {
        return PineconeEmbeddingStore.builder()
                .apiKey(System.getenv("PINECONE_API_KEY"))
                .index("interview-assistant-index") // 固定单个 Index
                .nameSpace(namespace) // 按命名空间隔离数据
                .build();
    }
}

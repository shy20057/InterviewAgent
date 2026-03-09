package com.atguigu.retriever;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ChromaDB 向量数据库提供者
 */
@Component
@ConditionalOnProperty(name = "vector.store.type", havingValue = "chroma")
public class ChromaEmbeddingStoreProvider implements EmbeddingStoreProvider {

    @Value("${chroma.baseUrl:http://localhost:8000}")
    private String chromaBaseUrl;

    @Override
    public EmbeddingStore<TextSegment> getStoreByNamespace(String namespace) {
        // ChromaDB 的 collectionName 不允许出现中划线 (需要 3-63 并且是字母/数字/下划线)
        String collectionName = namespace.replace("-", "_").toLowerCase();
        
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaBaseUrl)
                .collectionName(collectionName)
                .build();
    }
}

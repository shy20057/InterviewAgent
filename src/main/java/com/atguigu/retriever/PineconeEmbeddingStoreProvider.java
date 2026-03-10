package com.atguigu.retriever;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pinecone 向量数据库提供者
 */
@Component // 条件装配注解 只有当某个配置属性满足指定条件时，才创建这个 Bean；否则这个 Bean 根本不会注册到 Spring 容器中。
@ConditionalOnProperty(name = "vector.store.type", havingValue = "pinecone", matchIfMissing = true )
public class PineconeEmbeddingStoreProvider implements EmbeddingStoreProvider {

    private EmbeddingModel embeddingModel;
    public PineconeEmbeddingStoreProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingStore<TextSegment> getStoreByNamespace(String namespace) {
        return PineconeEmbeddingStore.builder()
                .apiKey(System.getenv("PINECONE_API_KEY"))
                .index("interview-assistant-index") // 固定单个 Index
                .nameSpace(namespace)                // 按命名空间隔离数据
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")               // 你在 Pinecone 选的 cloud
                        .region("us-east-1")        // 你在 Pinecone 选的 region
                        .dimension(embeddingModel.dimension()) // 向量维度和模型保持一致
                        .build())
                .build();
    }
    // 这里创建出来的是统一index的不同namespace,其实就是因为namespace是通过参数传进来的，本质一样
}

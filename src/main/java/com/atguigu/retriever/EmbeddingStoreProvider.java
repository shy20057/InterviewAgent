package com.atguigu.retriever;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 向量数据库提供者接口 (用于解耦 Pinecone 和 ChromaDB 等)
 */
public interface EmbeddingStoreProvider {
    /**
     * 根据 Namespace 或者 Collection 获取对应的 Vector Store 实例
     * @param namespace 命名空间/集合名称
     * @return 对应的 EmbeddingStore
     */
    EmbeddingStore<TextSegment> getStoreByNamespace(String namespace);
}

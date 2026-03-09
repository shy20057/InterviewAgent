package com.atguigu.retriever;

import com.atguigu.config.PineconeEmbeddingStoreConfig;
import dev.langchain4j.data.content.Content;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InterviewContentRetriever implements ContentRetriever {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private PineconeEmbeddingStoreConfig pineconeEmbeddingStoreConfig;

    public List<Content> retrieveForInterview(
            String query,
            String position,
            String difficulty,
            String userId) {

        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            String officialNamespace = getNamespaceByPosition(position);

            EmbeddingStore<TextSegment> officialStore = getStoreByNamespace(officialNamespace);

            Map<String, Object> metadataFilter = new HashMap<>();
            metadataFilter.put("difficulty", difficulty);
            metadataFilter.put("type", "official");

            EmbeddingSearchRequest officialRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5)
                    .minScore(0.7)
                    .filter(metadataFilter)
                    .build();

            EmbeddingSearchResult<TextSegment> officialResult = officialStore.search(officialRequest);
            List<EmbeddingMatch<TextSegment>> officialMatches = officialResult.matches();

            List<EmbeddingMatch<TextSegment>> resumeMatches = new ArrayList<>();
            if (userId != null && !userId.isEmpty()) {
                EmbeddingStore<TextSegment> resumeStore = getStoreByNamespace("user-resume-" + userId);

                EmbeddingSearchRequest resumeRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(3)
                        .minScore(0.6)
                        .build();

                EmbeddingSearchResult<TextSegment> resumeResult = resumeStore.search(resumeRequest);
                resumeMatches = resumeResult.matches();
            }

            List<Content> contents = new ArrayList<>();

            for (EmbeddingMatch<TextSegment> match : officialMatches) {
                contents.add(Content.from(
                    "[知识点] " + match.embedded().text()
                ));
            }

            for (EmbeddingMatch<TextSegment> match : resumeMatches) {
                contents.add(Content.from(
                    "[简历相关] " + match.embedded().text()
                ));
            }

            log.info("检索完成：官方题库 {} 条，用户简历 {} 条", officialMatches.size(), resumeMatches.size());

            return contents;

        } catch (Exception e) {
            log.error("检索失败", e);
            return new ArrayList<>();
        }
    }

    private String getNamespaceByPosition(String position) {
        switch (position) {
            case "java-backend":
                return "official-java-backend";
            case "frontend":
                return "official-frontend";
            case "fullstack":
            default:
                return "official-java-backend";
        }
    }

    private EmbeddingStore<TextSegment> getStoreByNamespace(String namespace) {
        return pineconeEmbeddingStoreConfig.getStoreByNamespace(namespace);
    }

    @Override
    public List<Content> retrieve(Query query) {
        return retrieveForInterview(query.text(), "java-backend", "medium", null);
    }
}

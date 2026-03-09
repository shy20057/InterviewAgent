package com.atguigu.retriever;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class InterviewContentRetriever {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStoreProvider embeddingStoreProvider;


    public List<Content> retrieveForInterview(
            String query,
            String position,
            String difficulty,
            String userId) {

        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            String officialNamespace = getNamespaceByPosition(position);

            EmbeddingStore<TextSegment> officialStore = getStoreByNamespace(officialNamespace);

            EmbeddingSearchRequest officialRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5)
                    .minScore(0.7)
                    .filter(MetadataFilterBuilder.metadataKey("difficulty").isEqualTo(difficulty)
                            .and(MetadataFilterBuilder.metadataKey("type").isEqualTo("official")))
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
        return embeddingStoreProvider.getStoreByNamespace(namespace);
    }


}

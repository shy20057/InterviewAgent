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

    public List<Content> retrieve4Answer(
            String answer,
            String position,
            List<String> skills,
            String difficulty,
            String userId) {

        try {

            // ===================1.根据用户的答案检索的知识点==========================//
            Embedding answerEmbedding = embeddingModel.embed(answer).content();

            String officialNamespace = getNamespaceByPosition(position);

            // 获取官方题库向量数据的namespace
            EmbeddingStore<TextSegment> officialStore = embeddingStoreProvider.getStoreByNamespace(officialNamespace);

            // 官方题库检索
            EmbeddingSearchRequest officialRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(answerEmbedding)
                    .maxResults(5)
                    .minScore(0.6)
                    .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("official"))
                    .build();

            EmbeddingSearchResult<TextSegment> officialResult = officialStore.search(officialRequest);
            // matches()获取 对象EmbeddingMatch 中有
            // -embedded()：匹配到的原始文字 -score()：相似度得分 -embeddingId()：该数据在数据库里的 ID
            List<EmbeddingMatch<TextSegment>> officialMatches = officialResult.matches();

            // ===================2.根据用户的答案检索的用户简历信息==========================//
            List<EmbeddingMatch<TextSegment>> resumeMatches = new ArrayList<>();
            if (userId != null && !userId.isEmpty()) {

                EmbeddingStore<TextSegment> resumeStore = embeddingStoreProvider
                        .getStoreByNamespace("user-resume-" + userId);

                EmbeddingSearchRequest resumeRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(answerEmbedding)
                        .maxResults(3) // 最多拿 3 条简历经历
                        .minScore(0.6)
                        .build();

                EmbeddingSearchResult<TextSegment> resumeResult = resumeStore.search(resumeRequest);
                resumeMatches = resumeResult.matches();
            }
            List<Content> contents = new ArrayList<>();

            for (EmbeddingMatch<TextSegment> match : officialMatches) {
                contents.add(Content.from(
                        "[知识点] " + match.embedded().text()));
            }

            for (EmbeddingMatch<TextSegment> match : resumeMatches) {
                contents.add(Content.from(
                        "[简历相关] " + match.embedded().text()));
            }

            log.info("检索完成：官方题库 {} 条，用户简历 {} 条", officialMatches.size(), resumeMatches.size());

            return contents;

        } catch (Exception e) {
            log.error("检索失败", e);
            return new ArrayList<>();
        }
    }

    /*查技术栈 项目经历相关知识库*/
    public List<Content> retrieve4InitQuestion(
            String position,
            List<String> skills,
            String projectExperience,
            String difficulty,
            String userId){

        Embedding contentEmbedding = embeddingModel.embed(projectExperience).content();
        String officialNamespace = getNamespaceByPosition(position);
        EmbeddingStore<TextSegment> officialStore = embeddingStoreProvider.getStoreByNamespace(officialNamespace);
        EmbeddingSearchRequest officialRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(contentEmbedding)
                .maxResults(8)
                .minScore(0.7)
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("official")
                        .and(MetadataFilterBuilder.metadataKey("skill").isIn(skills))
                )
                .build();

        EmbeddingSearchResult<TextSegment> officialResult = officialStore.search(officialRequest);
        List<EmbeddingMatch<TextSegment>> officialMatches = officialResult.matches();

        List<Content> contents = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : officialMatches) {
            contents.add(Content.from(
                     match.embedded().text()));
        }
        log.info("检索完成：官方题库 {} 条", officialMatches.size());
        return contents;
    }

    private String getNamespaceByPosition(String position) {
        switch (position) {
            case "后端":
                return "official-java-backend";
            case "前端":
                return "official-frontend";
            case "全栈":
            default:
                return "official-java-backend";
        }
    }

}

package com.atguigu.loader;

import com.atguigu.retriever.EmbeddingStoreProvider;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class QuestionDataLoader {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStoreProvider embeddingStoreProvider;

    @Value("${interview.questions.path:src/main/resources/content}")
    private String questionsPath;

    @Value("${interview.questions.auto-load:false}")
    private boolean autoLoad;

    @PostConstruct
    public void initOfficialQuestions() {
        if (!autoLoad) {
            log.info("题库自动加载已关闭（interview.questions.auto-load=false），如需加载题库请设置为 true");
            return;
        }

        log.info("开始初始化官方题库...");
        
        try {
            loadJavaBackendQuestions();
            loadFrontendQuestions();
            
            log.info("官方题库初始化完成！");
        } catch (Exception e) {
            log.error("题库初始化失败", e);
        }
    }

    private void loadJavaBackendQuestions() {
        String path = questionsPath + "/java后端";
        loadQuestionsFromDirectory(path, "official-java-backend", getJavaBackendCategoryMap());
    }

    private void loadFrontendQuestions() {
        String path = questionsPath + "/前端";
        loadQuestionsFromDirectory(path, "official-frontend", getFrontendCategoryMap());
    }

    private Map<String, String> getJavaBackendCategoryMap() {
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("Java 基础", "java-basic");
        categoryMap.put("Java 集合", "java-collection");
        categoryMap.put("Java 并发", "java-concurrent");
        categoryMap.put("Java 虚拟机", "jvm");
        categoryMap.put("MySQL", "mysql");
        categoryMap.put("Redis", "redis");
        categoryMap.put("Spring", "spring");
        categoryMap.put("SpringBoot", "springboot");
        categoryMap.put("SpringCloud", "springcloud");
        categoryMap.put("后端系统设计", "system-design");
        categoryMap.put("后端场景", "scenario");
        categoryMap.put("AI大模型", "ai-llm");
        categoryMap.put("Java 热门", "hot-questions");
        return categoryMap;
    }

    private Map<String, String> getFrontendCategoryMap() {
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("前端 HTML", "html");
        categoryMap.put("Vue", "vue");
        categoryMap.put("React", "react");
        categoryMap.put("Nginx", "nginx");
        categoryMap.put("前端热门", "hot-questions");
        return categoryMap;
    }

    private void loadQuestionsFromDirectory(String directoryPath, String namespace, Map<String, String> categoryMap) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                log.warn("目录不存在：{}", directoryPath);
                return;
            }

            EmbeddingStore<TextSegment> storeForNamespace = embeddingStoreProvider.getStoreByNamespace(namespace);

            File[] files = path.toFile().listFiles((dir, name) -> name.endsWith(".pdf"));
            if (files == null || files.length == 0) {
                log.warn("目录下未找到PDF文件：{}", directoryPath);
                return;
            }
            
            for (File file : files) {
                String fileName = file.getName();
                String category = determineCategory(fileName, categoryMap);
                String difficulty = determineDifficulty(fileName);
                
                loadQuestionsFromPdf(storeForNamespace, file.getAbsolutePath(), namespace, category, difficulty);
            }
            
        } catch (Exception e) {
            log.error("加载目录 {} 失败", directoryPath, e);
        }
    }

    private String determineCategory(String fileName, Map<String, String> categoryMap) {
        for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
            if (fileName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "general";
    }

    private String determineDifficulty(String fileName) {
        if (fileName.contains("基础") || fileName.contains("HTML")) {
            return "easy";
        } else if (fileName.contains("热门")) {
            return "medium";
        } else if (fileName.contains("系统设计") || fileName.contains("场景") || fileName.contains("AI大模型")) {
            return "hard";
        }
        return "medium";
    }

    private void loadQuestionsFromPdf(EmbeddingStore<TextSegment> store,
                                      String pdfPath,
                                      String namespace,
                                      String category,
                                      String difficulty) {
        try {
            log.info("正在加载：{}，分类：{}，难度：{}", pdfPath, category, difficulty);
            
            Document document = FileSystemDocumentLoader.loadDocument(pdfPath, new ApachePdfBoxDocumentParser());
            
            DocumentSplitter splitter = new dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter(300, 30);
            List<TextSegment> segments = splitter.split(document);
            
            int count = 0;
            for (TextSegment segment : segments) {
                Map<String, Object> metadataMap = new HashMap<>();
                metadataMap.put("category", category);
                metadataMap.put("difficulty", difficulty);
                metadataMap.put("source", new File(pdfPath).getName());
                metadataMap.put("type", "official");

                Metadata metadata = Metadata.from(metadataMap);
                TextSegment enrichedSegment = TextSegment.from(segment.text(), metadata);
                Response<Embedding> response = embeddingModel.embed(enrichedSegment);
                store.add(response.content(), enrichedSegment);
                
                count++;
            }
            
            log.info("成功加载 {} 道题到 namespace: {}", count, namespace);
            
        } catch (Exception e) {
            log.error("加载 PDF 失败：{}", pdfPath, e);
        }
    }
}

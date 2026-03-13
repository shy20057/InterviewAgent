package com.atguigu.loader;

import com.atguigu.retriever.EmbeddingStoreProvider;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
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

    // 向量数据库 index -> namespace -> vector + metadata
    // 存储的数据结构
   /* {
        "id": "uuid-12345",           // 唯一标识
        "vector": [0.1, -0.5, 0.3...], // 1536 维向量
        "metadata": {
        "category": "java-basic",     // 分类：Java 基础
        "difficulty": "easy",         // 难度：简单
        "source": "Java 基础.pdf",     // 来源文件
        "type": "official"            // 类型：官方题库
      },
        "text": "Java 的特点有哪些？..."  // 原始文本（TextSegment）
      }*/

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

            // 根据namespace获取向量存储空间
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
            
            // 文本预处理：清洗无关信息
            String originalText = document.text();
            String cleanedText = cleanText(originalText);
            
            // 使用清洗后的文本创建新文档进行分割
            Document cleanedDocument = Document.from(cleanedText, document.metadata());
            
            // 优化分割参数：1500 字符上限，200 字符重叠
            DocumentSplitter splitter = new DocumentByParagraphSplitter(1500, 200);
            List<TextSegment> segments = splitter.split(cleanedDocument);

            int count = 0;
            String fileName = new File(pdfPath).getName();

            // 抽样打印预览，用于验证分割效果
            if (!segments.isEmpty()) {
                String preview = segments.get(0).text();
                if (preview.length() > 200) preview = preview.substring(0, 200) + "...";
                log.info("文件 {} 分割完成，共 {} 个片段。初步预览第一个片段内容：\n---[START]---\n{}\n---[END]---", 
                        fileName, segments.size(), preview);
            }

            for (TextSegment segment : segments) {
                Map<String, Object> metadataMap = new HashMap<>();
                metadataMap.put("category", category);
                // metadataMap.put("difficulty", difficulty);
                metadataMap.put("source", fileName);
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

    /**
     * 清洗文本，去除无用的页眉页脚和推广信息
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // 1. 去除面试鸭相关的页眉页脚和推广链接
        String cleaned = text.replaceAll("mianshiya\\.com", "");
        cleaned = cleaned.replaceAll("https?://(www\\.)?mianshiya\\.com[^\\s]*", "");
        cleaned = cleaned.replaceAll("本资源来自面试鸭：[^\\n]*", "");

        // 2. 去除“推荐更多免费学编程资源”及其列表内容
        cleaned = cleaned.replaceAll("推荐更多免费学编程资源：[\\s\\S]*?随时随地提升面试能力", "");

        // 3. 去除 PDF 解析可能产生的连续空行
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned.trim();
    }
}

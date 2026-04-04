package com.zixin.aicapabilityprovider.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 血糖健康知识库服务
 *
 * 启动时将糖尿病管理知识文件加载到内存向量库（SimpleVectorStore），
 * 支持语义相似度检索，为AI生成报告提供知识支撑（RAG）。
 */
@Slf4j
@Component
public class GlucoseKnowledgeService {

    private static final int CHUNK_SIZE = 800;
    private static final int TOP_K = 3;

    @Value("classpath:knowledge/glucose-diabetes-guidelines.md")
    private Resource guidelinesResource;

    private final SimpleVectorStore vectorStore;

    public GlucoseKnowledgeService(EmbeddingModel embeddingModel) {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 启动时加载知识库文件到向量库
     */
    @PostConstruct
    public void loadKnowledge() {
        try {
            String content = StreamUtils.copyToString(guidelinesResource.getInputStream(), StandardCharsets.UTF_8);
            List<Document> chunks = splitIntoChunks(content);
            vectorStore.add(chunks);
            log.info("GlucoseKnowledgeService - 知识库加载完成, chunks={}", chunks.size());
        } catch (Exception e) {
            log.error("GlucoseKnowledgeService - 知识库加载失败", e);
        }
    }

    /**
     * 根据查询语句检索相关知识片段
     *
     * @param query 查询语句（如：空腹血糖偏高的饮食建议）
     * @return 拼接后的相关知识文本
     */
    public String retrieve(String query) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(TOP_K).build()
            );
            if (results == null || results.isEmpty()) {
                return "";
            }
            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            log.warn("GlucoseKnowledgeService - 检索失败, query={}", query, e);
            return "";
        }
    }

    /**
     * 将长文本按段落分割为知识片段
     */
    private List<Document> splitIntoChunks(String content) {
        // 按二级标题拆分（## 开头的段落）
        String[] sections = content.split("(?m)^## ");
        return Arrays.stream(sections)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    String text = s.length() > CHUNK_SIZE ? s.substring(0, CHUNK_SIZE) : s;
                    return new Document("## " + text.trim());
                })
                .collect(Collectors.toList());
    }
}

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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 血糖健康知识库服务
 *
 * 启动时将糖尿病管理知识文件加载到内存向量库（SimpleVectorStore），
 * 支持语义相似度检索，为AI生成报告提供知识支撑（RAG）。
 *
 * 容错策略：
 * - 向量检索可用时：使用语义相似度检索（精度高）
 * - 向量检索不可用时：自动降级为关键词匹配检索（保证可用性）
 */
@Slf4j
@Component
public class GlucoseKnowledgeService {

    private static final int CHUNK_SIZE = 800;
    private static final int TOP_K = 3;

    @Value("classpath:knowledge/glucose-diabetes-guidelines.md")
    private Resource guidelinesResource;

    private final SimpleVectorStore vectorStore;
    private final AtomicBoolean vectorStoreReady = new AtomicBoolean(false);

    /** 关键词匹配降级用的知识片段缓存（向量库不可用时的兜底方案） */
    private volatile List<Document> fallbackChunks = Collections.emptyList();
    private volatile String fullKnowledgeText = "";

    public GlucoseKnowledgeService(EmbeddingModel embeddingModel) {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 启动时加载知识库文件到向量库；
     * 若向量化失败，自动降级为关键词匹配模式。
     */
    @PostConstruct
    public void loadKnowledge() {
        try {
            String content = StreamUtils.copyToString(guidelinesResource.getInputStream(), StandardCharsets.UTF_8);
            fullKnowledgeText = content;
            List<Document> chunks = splitIntoChunks(content);
            fallbackChunks = chunks;

            vectorStore.add(chunks);
            vectorStoreReady.set(true);
            log.info("GlucoseKnowledgeService - 知识库向量化加载完成, chunks={}", chunks.size());
        } catch (Exception e) {
            vectorStoreReady.set(false);
            log.warn("GlucoseKnowledgeService - 向量化加载失败，自动降级为关键词匹配检索 ({})", e.getMessage());
            // fallbackChunks 和 fullKnowledgeText 已在上面赋值，可直接用于关键词检索
        }
    }

    /**
     * 根据查询语句检索相关知识片段
     *
     * 优先使用语义向量检索，失败时自动降级为关键词匹配。
     *
     * @param query 查询语句（如：空腹血糖偏高的饮食建议）
     * @return 拼接后的相关知识文本
     */
    public String retrieve(String query) {
        // 策略1：向量检索（高精度语义匹配）
        if (vectorStoreReady.get()) {
            try {
                List<Document> results = vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).topK(TOP_K).build()
                );
                if (results != null && !results.isEmpty()) {
                    return results.stream()
                            .map(Document::getText)
                            .collect(Collectors.joining("\n\n---\n\n"));
                }
            } catch (Exception e) {
                log.warn("GlucoseKnowledgeService - 向量检索异常，降级为关键词匹配, query={}, error={}",
                        query, e.getMessage());
                // 发生异常时标记不可用，避免后续请求继续失败
                vectorStoreReady.set(false);
            }
        }

        // 策略2：关键词匹配降级检索（保证系统可用）
        return keywordRetrieve(query);
    }

    /**
     * 基于关键词匹配的检索降级方案。
     *
     * 当 Embedding 服务不可用时（如 API 404、网络不通等），
     * 通过对查询词进行分词，匹配知识片段标题进行简单检索。
     */
    private String keywordRetrieve(String query) {
        if (query == null || query.isBlank() || fallbackChunks.isEmpty()) {
            return "";
        }

        // 提取查询中的关键词（按常见血糖概念分词）
        Set<String> keywords = extractKeywords(query);

        // 按匹配分数排序，取 topK
        List<Document> matched = fallbackChunks.stream()
                .map(doc -> new AbstractMap.SimpleEntry<>(doc, matchScore(doc.getText(), keywords)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(TOP_K)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            // 无法匹配时返回知识库的前 TOP_K 段作为通用参考
            log.debug("GlucoseKnowledgeService - 关键词无匹配，返回通用知识片段");
            return fallbackChunks.stream()
                    .limit(TOP_K)
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
        }

        log.debug("GlucoseKnowledgeService - 关键词匹配检索, query={}, matched={}", query, matched.size());
        return matched.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 从查询语句中提取关键词
     */
    private Set<String> extractKeywords(String query) {
        Set<String> keywords = new LinkedHashSet<>();
        // 血糖状态关键词
        for (String kw : new String[]{"空腹", "餐前", "餐后", "低血糖", "高血糖", "偏高", "正常", "严重"}) {
            if (query.contains(kw)) keywords.add(kw);
        }
        // 建议类型关键词
        for (String kw : new String[]{"饮食", "运动", "用药", "胰岛素", "管理", "建议", "风险", "处理", "预防"}) {
            if (query.contains(kw)) keywords.add(kw);
        }
        return keywords;
    }

    /**
     * 计算文档文本与关键词集合的匹配分数
     */
    private int matchScore(String text, Set<String> keywords) {
        int score = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) score++;
        }
        return score;
    }

    /**
     * 知识库是否已成功加载（向量化或降级模式均视为可用）
     */
    public boolean isAvailable() {
        return !fallbackChunks.isEmpty();
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

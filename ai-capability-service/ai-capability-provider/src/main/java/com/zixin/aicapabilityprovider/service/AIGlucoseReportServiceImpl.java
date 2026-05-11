package com.zixin.aicapabilityprovider.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.zixin.aicapabilityapi.api.AIGlucoseReportAPI;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportRequest;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportResponse;
import com.zixin.aicapabilityprovider.knowledge.GlucoseKnowledgeService;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 血糖健康报告生成服务
 *
 * 流程：
 * 1. 构建血糖数据摘要
 * 2. 通过RAG检索相关糖尿病知识
 * 3. 将血糖数据 + RAG知识注入Prompt
 * 4. 调用AI生成结构化健康报告
 */
@Slf4j
@Service
@DubboService
public class AIGlucoseReportServiceImpl implements AIGlucoseReportAPI {

    private final ChatClient chatClient;
    private final GlucoseKnowledgeService knowledgeService;
    private final Gson gson = new GsonBuilder().create();

    public AIGlucoseReportServiceImpl(
            @Qualifier("glucoseReportChatClient") ChatClient chatClient,
            GlucoseKnowledgeService knowledgeService) {
        this.chatClient = chatClient;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public GenerateGlucoseReportResponse generateHealthReport(GenerateGlucoseReportRequest request) {
        GenerateGlucoseReportResponse response = new GenerateGlucoseReportResponse();

        if (request == null || request.getPatientId() == null) {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("患者ID不能为空");
            return response;
        }

        try {
            // 1. RAG：构建查询语句，检索相关知识（失败不影响主流程，有降级方案）
            String ragQuery = buildRagQuery(request);
            String knowledgeContext = knowledgeService.retrieve(ragQuery);
            boolean hasKnowledge = knowledgeContext != null && !knowledgeContext.isBlank();
            log.debug("AIGlucoseReport - RAG retrieved {} chars, hasKnowledge={}, patientId={}",
                    hasKnowledge ? knowledgeContext.length() : 0, hasKnowledge, request.getPatientId());

            // 2. 构建用户Prompt（RAG知识缺失时仍能生成有效报告）
            String userPrompt = buildUserPrompt(request, knowledgeContext);

            // 3. 调用AI
            String modelResponse = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
            log.info("AIGlucoseReport - AI response received, length={}, patientId={}",
                    modelResponse != null ? modelResponse.length() : 0, request.getPatientId());

            // 4. 解析响应（含多级容错）
            parseAndFill(modelResponse, response, request);

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("健康报告生成成功");

        } catch (Exception e) {
            log.error("AIGlucoseReport - 生成失败, patientId={}", request.getPatientId(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("健康报告生成失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 根据血糖状态构建RAG检索语句
     */
    private String buildRagQuery(GenerateGlucoseReportRequest request) {
        StringBuilder sb = new StringBuilder();
        String mealLabel = mealLabel(request.getMealType());
        sb.append(mealLabel).append("血糖");

        if (request.getMaxPredictedMmol() != null) {
            double max = request.getMaxPredictedMmol();
            if (max > 13.9) {
                sb.append("严重高血糖风险饮食运动建议");
            } else if (max > 10.0) {
                sb.append("偏高血糖控制建议");
            } else if (max < 3.9) {
                sb.append("低血糖预防处理建议");
            } else {
                sb.append("正常范围管理建议");
            }
        } else {
            sb.append("管理建议");
        }
        return sb.toString();
    }

    /**
     * 构建用户Prompt，包含血糖数据和RAG知识
     */
    private String buildUserPrompt(GenerateGlucoseReportRequest request, String knowledgeContext) {
        StringBuilder sb = new StringBuilder();
        String today = LocalDate.now().toString();

        sb.append("# 血糖健康报告生成任务\n\n");
        sb.append("**报告日期**: ").append(today).append("\n");
        sb.append("**患者ID**: ").append(request.getPatientId()).append("\n");
        sb.append("**用餐状态**: ").append(mealLabel(request.getMealType())).append("\n\n");

        // 血糖数据
        sb.append("## Observation · 血糖监测数据\n\n");
        appendGlucoseData(sb, request);

        // RAG知识
        if (knowledgeContext != null && !knowledgeContext.isBlank()) {
            sb.append("\n## Observation · 参考知识库\n\n");
            sb.append(knowledgeContext).append("\n");
        }

        sb.append("\n---\n");
        sb.append("请根据以上血糖数据和参考知识，生成个性化健康报告。\n");
        sb.append("**关键要求**：\n");
        sb.append("1. 输出严格按照指定JSON格式，日期字段用: ").append(today).append("\n");
        sb.append("2. fullReportContent内容精炼在500字以内，确保JSON完整闭合，不截断\n");
        sb.append("3. healthSuggestions控制在100字以内\n");
        sb.append("4. JSON中所有字符串值内的双引号必须转义为\\\"，换行用\\n表示\n");

        return sb.toString();
    }

    private void appendGlucoseData(StringBuilder sb, GenerateGlucoseReportRequest request) {
        if (request.getAvgCurrentMmol() != null) {
            sb.append("- 当前血糖均值: ").append(String.format("%.2f mmol/L", request.getAvgCurrentMmol())).append("\n");
        }
        if (request.getMaxPredictedMmol() != null) {
            sb.append("- 预测血糖峰值: ").append(String.format("%.2f mmol/L", request.getMaxPredictedMmol())).append("\n");
        }
        if (request.getConfidence() != null) {
            sb.append("- 预测置信度: ").append(String.format("%.0f%%", request.getConfidence() * 100)).append("\n");
        }
        sb.append("- 是否触发预警: ").append(Boolean.TRUE.equals(request.getAlertTriggered()) ? "是" : "否").append("\n");

        List<Double> predicted = request.getPredictedGlucoseValues();
        if (predicted != null && !predicted.isEmpty()) {
            sb.append("- 预测血糖序列(mg/dL): ").append(predicted).append("\n");
        }
        List<Double> current = request.getCurrentGlucoseValues();
        if (current != null && !current.isEmpty()) {
            int size = Math.min(current.size(), 12);
            sb.append("- 近期CGM序列(mg/dL，最近").append(size).append("条): ")
              .append(current.subList(current.size() - size, current.size())).append("\n");
        }
    }

    /**
     * 多级容错解析AI响应中的JSON内容。
     *
     * 策略优先级：
     * 1. 标准JSON解析 —— 最理想的情况
     * 2. JSON修复解析（补齐截断的大括号/引号） —— AI输出被截断时
     * 3. 正则字段提取 —— JSON语法严重损坏时
     * 4. 原始响应兜底 —— 以上均失败时
     */
    private void parseAndFill(String modelResponse, GenerateGlucoseReportResponse response,
                               GenerateGlucoseReportRequest request) {
        String today = LocalDate.now().toString();

        // 策略1：标准JSON解析
        String jsonStr = trimJsonFence(modelResponse);
        ReportContent content = tryParseStandard(jsonStr);
        if (content != null) {
            applyContent(response, content, today);
            return;
        }

        // 策略2：JSON修复解析（处理截断）
        String repairedJson = repairTruncatedJson(jsonStr);
        if (repairedJson != null) {
            content = tryParseStandard(repairedJson);
            if (content != null) {
                log.info("AIGlucoseReport - JSON修复解析成功");
                applyContent(response, content, today);
                return;
            }
        }

        // 策略3：正则字段提取（处理严重损坏的JSON）
        content = extractFieldsWithRegex(jsonStr);
        if (content != null && hasAnyField(content)) {
            log.info("AIGlucoseReport - 正则提取字段成功");
            applyContent(response, content, today);
            return;
        }
        // 如果trim后的不是JSON也尝试从原始响应中正则提取
        if (!jsonStr.equals(modelResponse)) {
            content = extractFieldsWithRegex(modelResponse);
            if (content != null && hasAnyField(content)) {
                log.info("AIGlucoseReport - 从原始响应正则提取字段成功");
                applyContent(response, content, today);
                return;
            }
        }

        // 策略4：原始响应兜底
        log.warn("AIGlucoseReport - 所有JSON解析策略失败，使用原始响应兜底");
        response.setReportTitle("血糖健康分析报告（" + today + "）");
        response.setHealthSuggestions("请参阅完整报告获取健康建议。");
        response.setFullReportContent(modelResponse);
    }

    /**
     * 标准Gson解析
     */
    private ReportContent tryParseStandard(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            return gson.fromJson(jsonStr, ReportContent.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 修复被截断的JSON字符串。
     *
     * AI模型在生成长文本时可能在字段值中间截断，常见截断形式：
     * - 缺少闭合 }：{"title":"abc","content":"xxxx...  → 补齐"}
     * - 缺少闭合 "：{"title":"abc","content":"xxxx...  → 补齐"}
     * - 缺少闭合 ]：{"arr":[1,2,...  → 补齐]}
     *
     * @return 修复后的JSON字符串，失败返回null
     */
    private String repairTruncatedJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        String s = jsonStr.trim();

        // 不是JSON开头则不处理
        if (!s.startsWith("{")) return null;

        try {
            // 先试一下是不是本来就合法
            JsonParser.parseString(s);
            return s; // 无需修复
        } catch (JsonSyntaxException ignored) {
            // 需要修复
        }

        StringBuilder sb = new StringBuilder(s);
        int openBraces = 0, closeBraces = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') openBraces++;
            if (c == '}') closeBraces++;
        }

        // 如果最后一个非空白字符是字符串内的逗号，去掉它
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }

        // 如果在字符串内被截断，补一个引号
        if (inString) {
            // 去掉可能不完整的转义
            if (sb.charAt(sb.length() - 1) == '\\') {
                sb.setLength(sb.length() - 1);
            }
            sb.append('"');
        }

        // 补齐缺失的闭合大括号
        while (closeBraces < openBraces) {
            sb.append('}');
            closeBraces++;
        }

        // 验证修复结果
        try {
            JsonParser.parseString(sb.toString());
            return sb.toString();
        } catch (JsonSyntaxException e) {
            log.debug("AIGlucoseReport - JSON修复失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 使用正则表达式从损坏的JSON中提取字段值。
     * 适用于JSON语法严重损坏但关键字段仍可识别的情况。
     */
    private ReportContent extractFieldsWithRegex(String text) {
        if (text == null || text.isBlank()) return null;
        ReportContent content = new ReportContent();

        content.setReportTitle(extractJsonStringField(text, "reportTitle"));
        content.setHealthSuggestions(extractJsonStringField(text, "healthSuggestions"));
        content.setFullReportContent(extractJsonStringField(text, "fullReportContent"));

        return content;
    }

    /**
     * 用正则从JSON文本中提取指定字符串字段的值。
     * 支持：\"fieldName\":\"value\" 和 \"fieldName\": \"value\"
     */
    private String extractJsonStringField(String text, String fieldName) {
        // 匹配 "fieldName": "..." 或 "fieldName":"..."
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                Pattern.DOTALL
        );
        Matcher m = p.matcher(text);
        if (m.find()) {
            String value = m.group(1);
            // 还原转义字符
            return value.replace("\\\"", "\"")
                       .replace("\\n", "\n")
                       .replace("\\t", "\t")
                       .replace("\\r", "\r")
                       .replace("\\\\", "\\");
        }
        return null;
    }

    /**
     * 检查 ReportContent 是否有至少一个非空字段
     */
    private boolean hasAnyField(ReportContent content) {
        return content.getReportTitle() != null
                || content.getHealthSuggestions() != null
                || content.getFullReportContent() != null;
    }

    /**
     * 将解析结果填充到响应对象，缺失字段用默认值
     */
    private void applyContent(GenerateGlucoseReportResponse response, ReportContent content, String today) {
        response.setReportTitle(
                content.getReportTitle() != null ? content.getReportTitle() : "血糖健康分析报告（" + today + "）"
        );
        response.setHealthSuggestions(
                content.getHealthSuggestions() != null ? content.getHealthSuggestions() : "请参阅完整报告获取健康建议。"
        );
        response.setFullReportContent(
                content.getFullReportContent() != null ? content.getFullReportContent() : ""
        );
    }

    private String mealLabel(Integer mealType) {
        if (mealType == null) return "空腹";
        return switch (mealType) {
            case 2 -> "餐前";
            case 3 -> "餐后";
            default -> "空腹";
        };
    }

    private String trimJsonFence(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) s = s.substring(firstNl + 1);
            int endFence = s.lastIndexOf("```");
            if (endFence >= 0) s = s.substring(0, endFence);
            return s.trim();
        }
        return s;
    }

    @lombok.Data
    private static class ReportContent {
        private String reportTitle;
        private String healthSuggestions;
        private String fullReportContent;
    }
}

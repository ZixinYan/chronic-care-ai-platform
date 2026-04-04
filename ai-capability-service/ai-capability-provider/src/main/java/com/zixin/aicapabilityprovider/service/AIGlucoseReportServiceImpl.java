package com.zixin.aicapabilityprovider.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
            // 1. RAG：构建查询语句，检索相关知识
            String ragQuery = buildRagQuery(request);
            String knowledgeContext = knowledgeService.retrieve(ragQuery);
            log.debug("AIGlucoseReport - RAG retrieved {} chars, patientId={}", knowledgeContext.length(), request.getPatientId());

            // 2. 构建用户Prompt
            String userPrompt = buildUserPrompt(request, knowledgeContext);

            // 3. 调用AI
            String modelResponse = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
            log.info("AIGlucoseReport - AI response received, patientId={}", request.getPatientId());

            // 4. 解析响应
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
        sb.append("请根据以上血糖数据和参考知识，生成个性化健康报告。输出严格按照指定JSON格式，日期字段用: ").append(today).append("\n");

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

    private void parseAndFill(String modelResponse, GenerateGlucoseReportResponse response,
                               GenerateGlucoseReportRequest request) {
        String jsonStr = trimJsonFence(modelResponse);
        try {
            ReportContent content = gson.fromJson(jsonStr, ReportContent.class);
            if (content != null) {
                response.setReportTitle(content.getReportTitle());
                response.setHealthSuggestions(content.getHealthSuggestions());
                response.setFullReportContent(content.getFullReportContent());
                return;
            }
        } catch (Exception e) {
            log.warn("AIGlucoseReport - JSON解析失败，使用原始响应", e);
        }
        // 解析失败兜底
        String today = LocalDate.now().toString();
        response.setReportTitle("血糖健康分析报告（" + today + "）");
        response.setHealthSuggestions("请参阅完整报告获取健康建议。");
        response.setFullReportContent(modelResponse);
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

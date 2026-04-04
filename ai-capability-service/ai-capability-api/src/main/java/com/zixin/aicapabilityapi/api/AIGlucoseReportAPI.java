package com.zixin.aicapabilityapi.api;

import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportRequest;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportResponse;

/**
 * AI 血糖健康报告生成 API
 *
 * 基于血糖预测数据，结合RAG知识库，生成个性化健康报告和建议
 */
public interface AIGlucoseReportAPI {

    /**
     * 生成血糖健康报告
     *
     * 功能说明:
     * 1. 基于当前和预测血糖值进行分析
     * 2. 通过RAG检索糖尿病管理知识库获取相关知识
     * 3. 调用AI模型生成个性化健康建议和报告
     *
     * @param request 生成请求（含血糖数据和预测结果）
     * @return 健康报告（含标题、建议和完整报告内容）
     */
    GenerateGlucoseReportResponse generateHealthReport(GenerateGlucoseReportRequest request);
}

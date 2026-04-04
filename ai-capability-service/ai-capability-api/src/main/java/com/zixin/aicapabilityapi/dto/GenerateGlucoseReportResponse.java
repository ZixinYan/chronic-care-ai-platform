package com.zixin.aicapabilityapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AI 血糖健康报告生成响应
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GenerateGlucoseReportResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 报告标题
     */
    private String reportTitle;

    /**
     * 健康建议摘要（简短）
     */
    private String healthSuggestions;

    /**
     * 完整报告正文（含分析+建议）
     */
    private String fullReportContent;
}

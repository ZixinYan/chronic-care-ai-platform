package com.zixin.aicapabilityapi.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 血糖健康报告生成请求
 */
@Data
public class GenerateGlucoseReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 当前CGM血糖值列表 (mg/dL)
     */
    private List<Double> currentGlucoseValues;

    /**
     * 预测血糖值列表 (mg/dL)
     */
    private List<Double> predictedGlucoseValues;

    /**
     * 用餐状态: 1=空腹, 2=餐前, 3=餐后
     */
    private Integer mealType;

    /**
     * 预测血糖最大值 (mmol/L)
     */
    private Double maxPredictedMmol;

    /**
     * 当前血糖平均值 (mmol/L)
     */
    private Double avgCurrentMmol;

    /**
     * 预测置信度 (0~1)
     */
    private Double confidence;

    /**
     * 是否触发血糖预警
     */
    private Boolean alertTriggered;
}

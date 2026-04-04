package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 生成AI健康报告请求
 */
@Data
public class GenerateAIReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 当前血糖值列表（原始CGM数据）
     */
    private List<Double> currentGlucoseValues;

    /**
     * 当前血糖记录时间戳列表
     */
    private List<Long> currentGlucoseTimes;

    /**
     * 血糖预测数据
     */
    private List<Double> predictedGlucoseValues;

    /**
     * 预测时间点列表
     */
    private List<Long> predictedTimes;

    /**
     * 预测起始时间
     */
    private Long predictStartTime;

    /**
     * 数据类型：1-空腹, 2-餐后1h, 3-餐后2h, 4-餐后3h
     */
    private Integer mealType;

    /**
     * 原始数据维度信息
     */
    private GlucoseDataDimensions dataDimensions;

    /**
     * 预测置信度
     */
    private Double confidence;
}
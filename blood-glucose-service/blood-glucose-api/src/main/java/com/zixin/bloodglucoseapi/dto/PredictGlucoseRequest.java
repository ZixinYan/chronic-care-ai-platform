package com.zixin.bloodglucoseapi.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 血糖预测请求
 */
@Data
public class PredictGlucoseRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 校准血糖值 (CGM) - mg/dL
     */
    private List<Double> cbg;
    
    /**
     * 指尖血血糖值 - mg/dL
     */
    private List<Double> finger;
    
    /**
     * 基础率 - U/h
     */
    private List<Double> basal;
    
    /**
     * 心率 - bpm
     */
    private List<Double> hr;
    
    /**
     * 皮肤电反应
     */
    private List<Double> gsr;
    
    /**
     * 碳水化合物摄入 - 克
     */
    private List<Double> carbInput;
    
    /**
     * 大剂量胰岛素 - U
     */
    private List<Double> bolus;
    
    /**
     * 用餐状态：1-空腹, 2-餐前, 3-餐后
     */
    private Integer mealStatus;
    
    /**
     * 预测时长（小时）
     */
    private Integer predictHours = 3;
}
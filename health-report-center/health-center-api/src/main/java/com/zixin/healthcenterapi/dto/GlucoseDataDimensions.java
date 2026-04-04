package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 血糖数据维度信息
 */
@Data
public class GlucoseDataDimensions implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 校准血糖值 (CGM)
     */
    private List<Double> cbg;
    
    /**
     * 指尖血血糖值
     */
    private List<Double> finger;
    
    /**
     * 基础率
     */
    private List<Double> basal;
    
    /**
     * 心率
     */
    private List<Double> hr;
    
    /**
     * 皮肤电反应
     */
    private List<Double> gsr;
    
    /**
     * 碳水化合物摄入
     */
    private List<Double> carbInput;
    
    /**
     * 大剂量胰岛素
     */
    private List<Double> bolus;
}
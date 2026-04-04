package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 血糖阈值检查请求
 */
@Data
public class CheckGlucoseAlertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * CBG值 (mg/dL)
     */
    private Double cbgValue;
    
    /**
     * 用餐类型：1-空腹, 2-餐后1h, 3-餐后2h, 4-餐后3h
     */
    private Integer mealType;
}
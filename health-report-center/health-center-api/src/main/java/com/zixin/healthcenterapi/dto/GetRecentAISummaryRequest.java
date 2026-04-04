package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询最近AI总结请求
 */
@Data
public class GetRecentAISummaryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 查询天数（默认10）
     */
    private Integer days = 10;
}
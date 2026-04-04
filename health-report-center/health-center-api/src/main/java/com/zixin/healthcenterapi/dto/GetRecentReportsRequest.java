package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询最近报告请求
 */
@Data
public class GetRecentReportsRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 获取数量限制（默认5）
     */
    private Integer limit = 5;
}
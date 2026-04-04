package com.zixin.healthcenterapi.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * AI总结VO
 */
@Data
public class AISummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 总结ID
     */
    private Long summaryId;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 总结日期
     */
    private String summaryDate;
    
    /**
     * 总结内容
     */
    private String content;
    
    /**
     * 生成时间
     */
    private Long createTime;
}
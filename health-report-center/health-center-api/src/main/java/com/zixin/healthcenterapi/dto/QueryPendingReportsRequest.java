package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医生查询待审批报告请求
 * 
 * @author zixin
 */
@Data
public class QueryPendingReportsRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 医生ID (主治医生ID)
     */
    private Long doctorId;
    
    /**
     * 报告类型 (可选)
     */
    private Integer reportType;
    
    /**
     * 报告分类 (可选)
     */
    private String category;
    
    /**
     * 页码 (默认1)
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量 (默认10)
     */
    private Integer pageSize = 10;
}

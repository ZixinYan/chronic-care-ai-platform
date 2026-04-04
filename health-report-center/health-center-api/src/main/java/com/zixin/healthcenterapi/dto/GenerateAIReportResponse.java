package com.zixin.healthcenterapi.dto;

import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;

/**
 * 生成AI健康报告响应
 */
@Data
public class GenerateAIReportResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 生成的报告ID
     */
    private Long reportId;
    
    /**
     * 生成的报告信息
     */
    private HealthReportVO report;
    
    /**
     * 是否触发预警
     */
    private Boolean alertTriggered;
    
    /**
     * 预警信息
     */
    private String alertMessage;
}
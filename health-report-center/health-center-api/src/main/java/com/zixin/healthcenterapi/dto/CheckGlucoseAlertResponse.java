package com.zixin.healthcenterapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;

/**
 * 血糖阈值检查响应
 */
@Data
public class CheckGlucoseAlertResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否超过阈值
     */
    private Boolean exceeded;
    
    /**
     * CBG值 (mmol/L)
     */
    private Double cbgMmol;
    
    /**
     * 阈值
     */
    private Double threshold;
    
    /**
     * 是否发送短信成功
     */
    private Boolean smsSent;
}
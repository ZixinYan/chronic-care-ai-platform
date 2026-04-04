package com.zixin.healthcenterapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存文字报告响应（轻量版）
 */
@Data
public class SaveTextReportResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 生成的报告ID
     */
    private Long reportId;
}

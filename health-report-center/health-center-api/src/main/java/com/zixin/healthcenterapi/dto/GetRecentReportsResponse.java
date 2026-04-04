package com.zixin.healthcenterapi.dto;

import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询最近报告响应
 */
@Data
public class GetRecentReportsResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 最近报告列表
     */
    private List<HealthReportVO> reports;
}
package com.zixin.healthcenterapi.dto;

import com.zixin.healthcenterapi.vo.AISummaryVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询最近AI总结响应
 */
@Data
public class GetRecentAISummaryResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * AI总结列表
     */
    private List<AISummaryVO> summaries;
}
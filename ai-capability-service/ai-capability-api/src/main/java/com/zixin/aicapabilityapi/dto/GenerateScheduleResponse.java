package com.zixin.aicapabilityapi.dto;

import com.zixin.aicapabilityapi.vo.SuggestScheduleVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;


/**
 * AI生成日程建议响应
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GenerateScheduleResponse extends BaseResponse implements Serializable {
    private final static long serialVersionUID = 1L;
    
    /**
     * AI推荐的日程列表
     */
    private List<SuggestScheduleVO> recommendedSchedules;
    
    /**
     * AI推荐理由
     */
    private String recommendation;
}

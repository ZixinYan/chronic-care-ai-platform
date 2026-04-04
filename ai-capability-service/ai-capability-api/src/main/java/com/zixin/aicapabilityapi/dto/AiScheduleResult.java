package com.zixin.aicapabilityapi.dto;

import com.zixin.aicapabilityapi.vo.SuggestScheduleVO;
import lombok.Data;

import java.util.List;

/**
 * AI 日程推荐模型输出的中间结果，用于从大模型 JSON 解析到强类型对象。
 */

@Data
public class AiScheduleResult {

    /**
     * AI 推荐的日程列表
     */
    private List<SuggestScheduleVO> schedules;

    /**
     * 推荐理由
     */
    private String recommendation;
}


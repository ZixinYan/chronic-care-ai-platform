package com.zixin.aicapabilityapi.dto;



import lombok.Data;

import java.io.Serializable;

/**
 * AI生成日程建议请求
 */
@Data
public class GenerateScheduleRequest implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 预约日期 (格式: YYYY-MM-DD)
     */
    private String scheduleDay;
    /**
     * 业务需求
     */
    private String businessRequirement;
    /**
     * 是否指定医生 (true: 指定医生, false: 不指定医生)
     */
    private Boolean specifyDoctor;
    /**
     * 医生ID (当specifyDoctor为true时必填) 对应UserId
     */
    private Long doctorId;
}

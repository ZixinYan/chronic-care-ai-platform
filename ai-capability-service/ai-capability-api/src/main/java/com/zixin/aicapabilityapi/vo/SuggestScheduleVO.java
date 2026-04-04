package com.zixin.aicapabilityapi.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 日程VO
 */
@Data
public class SuggestScheduleVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 日程id
     */
    private Long id;
    /**
     * 医生Id
     */
    private Long doctorId;
    /**
     * 医生姓名
     */
    private String doctorName;
    /**
     * 患者id
     */
    private Long patientId;
    /**
     * 患者姓名
     */
    private String patientName;
    /**
     * 日程内容
     */
    private String schedule;
    /**
     * 日程类别ID
     */
    private Integer scheduleCategory;
    
    /**
     * 日程类别名称
     */
    private String scheduleCategoryName;
    
    /**
     * 日程日期
     */
    private String scheduleDay;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 优先级描述
     */
    private String priorityDesc;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 执行结果
     */
    private String result;

    /**
     * 关联链接
     */
    private String link;

    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
}

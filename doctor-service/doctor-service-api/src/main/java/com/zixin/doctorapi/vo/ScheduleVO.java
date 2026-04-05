package com.zixin.doctorapi.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 日程VO
 */
@Data
public class ScheduleVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 日程id
     */
    private Long id;
    /**
     * 医生ID
     */
    private Long doctorId;
    /**
     * 医生姓名
     */
    private String doctorName;
    /**
     * 患者姓名
     */
    private String patientName;
    /**
     * 日程内容
     */
    private String schedule;
    /**
     * 日程类别
     */
    private String scheduleCategory;
    
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
     * 开始时间（毫秒时间戳）
     */
    private Long startTime;
    /**
     * 结束时间（毫秒时间戳）
     */
    private Long endTime;
    
    /**
     * 开始时间字符串（格式: HH:mm 或 HH:mm:ss）
     */
    private String startTimeStr;
    /**
     * 结束时间字符串（格式: HH:mm 或 HH:mm:ss）
     */
    private String endTimeStr;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

    /**
     * 诊断报告
     */
    private String diagnosisReport;

    /**
     * 处方信息
     */
    private String prescription;

    /**
     * 备注
     */
    private String notes;
}

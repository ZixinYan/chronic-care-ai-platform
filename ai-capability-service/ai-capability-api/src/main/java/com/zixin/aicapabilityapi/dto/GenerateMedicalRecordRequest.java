package com.zixin.aicapabilityapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 电子病历生成请求
 */
@Data
public class GenerateMedicalRecordRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日程ID
     */
    private Long scheduleId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 日程类别
     */
    private Integer scheduleCategory;

    /**
     * 日程类别名称
     */
    private String scheduleCategoryName;

    /**
     * 日程日期 (YYYY-MM-DD)
     */
    private String scheduleDay;

    /**
     * 日程内容/描述
     */
    private String scheduleContent;

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

    /**
     * 关联健康报告链接
     */
    private String healthReportLink;

    /**
     * 患者历史健康数据摘要 (可选)
     */
    private String patientHealthSummary;
}

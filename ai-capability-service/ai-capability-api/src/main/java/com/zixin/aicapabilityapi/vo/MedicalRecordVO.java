package com.zixin.aicapabilityapi.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 电子病历 VO
 */
@Data
public class MedicalRecordVO implements Serializable {
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
     * 就诊日期
     */
    private String visitDate;

    /**
     * 就诊类型
     */
    private String visitType;

    /**
     * 主诉
     */
    private String chiefComplaint;

    /**
     * 现病史
     */
    private String presentIllness;

    /**
     * 既往史
     */
    private String pastHistory;

    /**
     * 诊断意见
     */
    private String diagnosis;

    /**
     * 治疗建议
     */
    private String treatmentPlan;

    /**
     * 处方信息
     */
    private String prescription;

    /**
     * 注意事项
     */
    private String precautions;

    /**
     * 随访建议
     */
    private String followUpAdvice;

    /**
     * 完整病历内容 (Markdown格式)
     */
    private String fullContent;
}

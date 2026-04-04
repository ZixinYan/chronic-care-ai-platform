package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 保存文字报告请求（轻量版，仅入库，不触发排班）
 */
@Data
public class SaveTextReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 报告分类（如 blood_glucose_prediction）
     */
    private String category;

    /**
     * 报告标题
     */
    private String title;

    /**
     * 文字内容
     */
    private String textContent;

    /**
     * 报告日期 (yyyy-MM-dd)
     */
    private String reportDate;
}

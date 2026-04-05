package com.zixin.healthcenterapi.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 健康报告视图对象
 * 
 * @author zixin
 */
@Data
public class HealthReportVO implements Serializable {
    private final static long serialVersionUID = 1L;
    
    /**
     * 报告ID
     */
    private Long reportId;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 主治医生ID
     */
    private Long attendingDoctorId;
    
    /**
     * 关联的日程ID
     */
    private Long scheduleId;
    
    /**
     * 主治医生姓名
     */
    private String doctorName;
    
    /**
     * 报告类型 (1-图片, 2-文字, 3-PDF)
     */
    private Integer reportType;
    
    /**
     * 报告类型描述
     */
    private String reportTypeDesc;
    
    /**
     * 报告分类
     */
    private String category;
    
    /**
     * 报告分类描述
     */
    private String categoryDesc;
    
    /**
     * 报告标题
     */
    private String title;
    
    /**
     * 报告描述
     */
    private String description;
    
    /**
     * 报告文件URL
     */
    private String fileUrl;
    
    /**
     * 文字内容
     */
    private String textContent;
    
    /**
     * 报告日期
     */
    private Long reportDate;
    
    /**
     * 上传者ID
     */
    private Long uploaderId;
    
    /**
     * 上传者姓名
     */
    private String uploaderName;
    
    /**
     * 医疗机构名称
     */
    private String hospitalName;
    
    /**
     * 审核状态 (0-待审核, 1-已通过, 2-已拒绝)
     */
    private Integer status;
    
    /**
     * 审核状态描述
     */
    private String statusDesc;
    
    /**
     * 审核备注
     */
    private String auditRemark;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
}

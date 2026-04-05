package com.zixin.healthcenterapi.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 健康报告实体类
 * 
 * 功能说明:
 * 1. 存储用户上传的第三方健康报告
 * 2. 支持图片(PNG等)和文字类型报告
 * 3. 报告仅对患者本人和主治医生可见
 * 4. 支持报告分类管理
 * 
 * 使用场景:
 * - 体检报告上传
 * - 检验检查报告
 * - 影像报告
 * - 病理报告
 * 
 * @author zixin
 */
@Data
@TableName("health_report")
public class HealthReport {
    
    /**
     * 报告ID (主键)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long reportId;
    
    /**
     * 患者ID (关联patient表)
     */
    private Long patientId;
    
    /**
     * 主治医生ID (关联doctor表)
     * 用于权限控制,医生只能查看自己患者的报告
     */
    private Long attendingDoctorId;
    
    /**
     * 关联的日程ID
     * 发送报告给医生时创建的日程ID
     */
    private Long scheduleId;
    
    /**
     * 报告类型
     * 1 - IMAGE (图片报告)
     * 2 - TEXT (文字报告)
     * 3 - PDF (PDF文档)
     */
    private Integer reportType;
    
    /**
     * 报告分类
     * 例如: 体检报告、血液检查、影像检查、病理报告等
     */
    private String category;
    
    /**
     * 报告标题
     */
    private String title;
    
    /**
     * 报告描述/备注
     */
    private String description;
    
    /**
     * 报告文件URL (OSS存储路径)
     * 图片类型: 存储图片URL
     * PDF类型: 存储PDF URL
     */
    private String fileUrl;
    
    /**
     * 文字内容 (文字类型报告)
     * 存储结构化的报告文本内容
     */
    private String textContent;
    
    /**
     * 报告日期
     * 检查/体检的实际日期
     */
    private String reportDate;

    /**
     * 上传者ID (关联account表)
     * 可能是患者本人或医生代为上传
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
     * 审核状态
     * 0 - PENDING (待审核)
     * 1 - APPROVED (已通过)
     * 2 - REJECTED (已拒绝)
     */
    private Integer status;
    
    /**
     * 审核备注
     */
    private String auditRemark;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;
    
    /**
     * 删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    
    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}

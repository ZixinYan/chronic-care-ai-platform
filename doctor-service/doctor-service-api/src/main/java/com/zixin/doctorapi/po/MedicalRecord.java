package com.zixin.doctorapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 电子病历表
 *
 * 存储 AI 生成的电子病历记录
 */
@Data
@TableName("medical_record")
public class MedicalRecord {

    /**
     * 病历ID (主键)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联日程ID
     */
    private Long scheduleId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名（冗余字段）
     */
    private String doctorName;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 患者姓名（冗余字段）
     */
    private String patientName;

    /**
     * 就诊日期 (格式: YYYY-MM-DD)
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

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    private Integer version;

    /**
     * 扩展字段 (JSON格式)
     */
    private JSON ext;
}

package com.zixin.accountapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.zixin.utils.security.SensitiveDataEncryptHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 患者信息表
 * 
 * 扩展Account的用户信息，存储患者专属字段
 * 所有身份相关的信息都在account-management中管理
 * 
 * 敏感字段加密说明:
 * - emergencyPhone: 紧急联系人电话(已加密)
 */
@Data
@TableName(value = "care_platform_patient", autoResultMap = true)
public class Patient implements Serializable {
    private final static long serialVersionUID = 1L;
    
    /**
     * 患者ID (主键)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 账户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    private String username;
    
    /**
     * 主治医生ID
     * 用于健康报告权限控制等场景
     */
    private Long attendingDoctorId;

    /**
     * 医生姓名
     */
    private String attendingDoctorName;

    /**
     * 病史摘要
     */
    private String medicalHistory;
    
    /**
     * 过敏史
     */
    private String allergies;
    
    /**
     * 血型
     */
    private String bloodType;
    
    /**
     * 身高 (cm)
     */
    private Integer height;
    
    /**
     * 体重 (kg)
     */
    private Integer weight;
    
    /**
     * 紧急联系人
     */
    private String emergencyContact;
    
    /**
     * 紧急联系人电话(加密存储)
     */
    @TableField(typeHandler = SensitiveDataEncryptHandler.class)
    private String emergencyPhone;
    
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
     * 逻辑删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    private Integer dele;
    
    /**
     * 版本号 (乐观锁)
     */
    @Version
    private Integer version;
    
    /**
     * 扩展字段 (JSON格式)
     * 可以存储: 既往病史详情、用药记录等
     */
    private String ext;
}

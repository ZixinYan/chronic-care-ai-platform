package com.zixin.accountapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.zixin.utils.security.SensitiveDataEncryptHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 医生信息表
 * 
 * 扩展Account的用户信息，存储医生专属字段
 * 所有身份相关的信息都在account-management中管理
 * 
 * 敏感字段加密说明:
 * - certificationNumber: 执业证书编号(已加密)
 */
@Data
@TableName(value = "care_platform_doctor", autoResultMap = true)
public class Doctor implements Serializable {
    private final static long serialVersionUID = 1L;
    
    /**
     * 医生ID (主键)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 账户ID (关联account表的accountId)
     */
    private Long userId;

    /**
     * 医生姓名
     */
    private String username;

    /**
     * 科室
     */
    private String department;
    
    /**
     * 职称 (如主治医师、主任医师等)
     */
    private String title;
    
    /**
     * 工作经验 (年数)
     */
    private Integer experience;
    
    /**
     * 执业证书编号(加密存储)
     */
    @TableField(typeHandler = SensitiveDataEncryptHandler.class)
    private String certificationNumber;
    
    /**
     * 学历
     */
    private String education;
    
    /**
     * 简介
     */
    private String bio;
    
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
     * 可以存储: 擅长领域、出诊时间、联系方式等
     */
    private String ext;
}

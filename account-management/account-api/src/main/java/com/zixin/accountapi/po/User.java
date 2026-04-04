package com.zixin.accountapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.*;
import com.zixin.utils.security.FastJson2TypeHandler;
import com.zixin.utils.security.SensitiveDataEncryptHandler;
import lombok.Data;

/**
 * 用户账户表
 * 
 * 敏感字段加密说明:
 * - phone: 手机号(已加密)
 * - email: 邮箱(已加密)
 * - idCard: 身份证号(已加密)
 * 
 * 哈希字段说明:
 * - phoneHash: 手机号SHA256哈希值，用于登录查询
 * - idCardHash: 身份证号SHA256哈希值，用于登录查询
 */
@Data
@TableName(value = "`care_platform_user`", autoResultMap = true)
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;
    
    private String username;
    
    private String nickname;
    
    private Integer gender;
    
    private String password;
    /**
     * 手机号(加密存储)
     */
    @TableField(typeHandler = SensitiveDataEncryptHandler.class)
    private String phone;
    /**
     * 手机号SHA256哈希值(用于登录查询)
     * 注意: 此字段由Service层自动生成，不需要手动设置
     */
    private String phoneHash;
    /**
     * 邮箱(加密存储)
     */
    @TableField(typeHandler = SensitiveDataEncryptHandler.class)
    private String email;
    
    private String avatar;
    
    private String address;
    
    private Long birthday;
    /**
     * 身份证号(加密存储)
     */
    @TableField(typeHandler = SensitiveDataEncryptHandler.class)
    private String idCard;
    /**
     * 身份证号SHA256哈希值(用于登录查询)
     * 注意: 此字段由Service层自动生成，不需要手动设置
     */
    private String idCardHash;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;
    
    @TableLogic
    private Integer dele;
    
    @Version
    private Integer version;
    
    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSON ext;
}

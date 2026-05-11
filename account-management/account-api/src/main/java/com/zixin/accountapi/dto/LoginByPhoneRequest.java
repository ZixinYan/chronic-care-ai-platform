package com.zixin.accountapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 手机号登录请求DTO
 * 用于手机号+验证码登录场景
 */
@Data
public class LoginByPhoneRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 手机号(必填)
     */
    private String phone;
}

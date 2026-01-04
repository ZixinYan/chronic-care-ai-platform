package com.zixin.accountapi.dto;

import lombok.Data;

import java.util.Date;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String address;
    private Integer gender;
    private String idCard;
    private Date birthday;
}

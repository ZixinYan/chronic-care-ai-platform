package com.zixin.accountapi.dto;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

import java.util.Date;

@Data
public class LogInResponse {
    private boolean msg;
    private LogInUserDTO data;

    @Data
    public static class LogInUserDTO{
        private Long userId;
        private String username;
        private String nickname;
        private String phone;
        private String email;
        private Integer gender;
        private String avatarUrl;
        private String address;
        private Date birthday;
        private JSON ext;
    }
}

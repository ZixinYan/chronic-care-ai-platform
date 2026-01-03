package com.zixin.accountapi.dto;

import com.zixin.accountapi.po.User;
import lombok.Data;

import java.util.Date;

@Data
public class UpdateUserInfoRequest {
    private updateUserInfoDTO updateData;

    @Data
    public static class updateUserInfoDTO{
        private Long userId;
        private String nickname;
        private Integer gender;
        private String avatarUrl;
        private String address;
        private Date birthday;
    }
}

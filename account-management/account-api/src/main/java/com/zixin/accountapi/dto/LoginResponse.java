package com.zixin.accountapi.dto;

import com.alibaba.fastjson2.JSON;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginResponse extends BaseResponse {
    private LoginUserDTO data;

    @Data
    public static class LoginUserDTO {
        private Long userId;
        private String username;
        private String nickname;
        private String phone;
        private String email;
        private Integer gender;
        private String avatarUrl;
        private String address;
        private String IdCard;
        private Date birthday;
        private JSON ext;

        public LoginUserDTO(Long userId, String username, String nickname, String email, Integer gender, String avatarUrl, String address, Date birthday,String idcard, JSON ext) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.email = email;
            this.gender = gender;
            this.avatarUrl = avatarUrl;
            this.address = address;
            this.birthday = birthday;
            this.IdCard = idcard;
            this.ext = ext;
        }
    }
}

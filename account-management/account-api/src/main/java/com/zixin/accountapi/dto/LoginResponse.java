package com.zixin.accountapi.dto;

import com.alibaba.fastjson2.JSON;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private LoginUserDTO data;

    @Data
    public static class LoginUserDTO implements Serializable {
        private  static final long serialVersionUID = 1L;
        private Long userId;
        private String username;
        private String nickname;
        private String phone;
        private String email;
        private Integer gender;
        private String avatarUrl;
        private String address;
        private String IdCard;
        private Long birthday;
        private List<String> role;
        private Set<String> permission;
        private JSON ext;

        public LoginUserDTO(Long userId, String username, String nickname, String phone, String email, Integer gender, String avatarUrl, String address, Long birthday,String idcard, List<String> role, Set<String> permission, JSON ext) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.phone = phone;
            this.email = email;
            this.gender = gender;
            this.avatarUrl = avatarUrl;
            this.address = address;
            this.birthday = birthday;
            this.IdCard = idcard;
            this.role = role;
            this.permission = permission;
            this.ext = ext;
        }
    }
}

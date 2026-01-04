package com.zixin.accountapi.dto;

import com.alibaba.fastjson2.JSON;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetUserInfoResponse extends BaseResponse {
   private List<UserInfoDTO> users;


   @Data
   public static class UserInfoDTO{
       private String username;
       private String nickname;
       private Integer gender;
       private String phone;
       private String email;
       private String avatarUrl;
       private String address;
       private Date birthday;
       private Date createTime;
       private Date updateTime;
       private JSON ext;


   }
}

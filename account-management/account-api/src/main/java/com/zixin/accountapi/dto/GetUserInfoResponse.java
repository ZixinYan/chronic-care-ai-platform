package com.zixin.accountapi.dto;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.zixin.accountapi.po.User;
import lombok.Data;

import java.util.Date;

@Data
public class GetUserInfoResponse {
   private UserInfoDTO[] users;


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

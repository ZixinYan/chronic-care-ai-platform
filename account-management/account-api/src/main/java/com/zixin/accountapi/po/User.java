package com.zixin.accountapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.util.Date;

@Data
@TableName("`care_platform_user`")
public class User {
    private Long userId;
    private String username;
    private String nickname;
    private Integer gender;
    private String password;
    private String phone;
    private String email;
    private String avatarUrl;
    private String address;
    private Date birthday;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer dele;
    @Version
    private Integer version;
    private JSON ext;
}

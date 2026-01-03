package com.zixin.accountapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.util.Date;

@Data
@TableName("'care_platform_role'")
public class Role {
    private Long roleId;
    private String roleCode; // DOCTOR / PATIENT / FAMILY
    private String description;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer dele;
    @Version
    private Integer version;
    private JSON ext;
}

package com.zixin.accountapi.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("care_platform_user_role")
public class AccountRole {
    private Long accountRoleId;
    private Long userId;
    private Long roleId;
}

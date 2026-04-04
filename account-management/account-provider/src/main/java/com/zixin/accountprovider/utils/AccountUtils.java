package com.zixin.accountprovider.utils;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zixin.accountapi.po.User;
import com.zixin.accountprovider.consts.Enum;
import com.zixin.accountprovider.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

@Slf4j
public final class AccountUtils  extends ServiceImpl<UserMapper, User> {
    /**
     * 校验更新字符是否符合预期
     */
    public static boolean validateUpdateData(Map<String, Object> updateData) {
        return updateData.keySet()
                .stream()
                .allMatch(Enum.UpdateUserField::contains);
    }
}

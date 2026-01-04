package com.zixin.accountprovider.utils;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zixin.accountapi.po.Account;
import com.zixin.accountprovider.consts.Enum;
import com.zixin.accountprovider.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class AccountUtils  extends ServiceImpl<AccountMapper, Account> {
    /**
     * 校验更新字符是否符合预期
     */
    public boolean validateUpdateData(Map<String, Objects> updateData) {
        return updateData.keySet()
                .stream()
                .allMatch(Enum.UpdateUserField::contains);
    }
}

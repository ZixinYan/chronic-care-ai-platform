package com.zixin.accountprovider.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zixin.accountapi.po.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}

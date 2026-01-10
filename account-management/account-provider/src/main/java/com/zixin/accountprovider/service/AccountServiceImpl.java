package com.zixin.accountprovider.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.accountapi.dto.*;
import com.zixin.accountapi.po.Account;
import com.zixin.accountprovider.mapper.AccountMapper;
import com.zixin.accountprovider.utils.AccountUtils;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@DubboService
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountManagementAPI {
    private final AccountUtils accountUtils;

    AccountServiceImpl(AccountUtils accountUtils, AccountMapper accountMapper){
        this.accountUtils = accountUtils;
    }

    @Override
    public LoginResponse login(LoginRequest logInRequest) {
        String loginAccount = logInRequest.getLoginAccount();
        String password = logInRequest.getPassword();
        Account account = new Account();
        LoginResponse loginResponse = new LoginResponse();
        // 根据手机号和用户名获取账户信息
        try {
            account = this.baseMapper.selectOne(new QueryWrapper<Account>()
                    .eq("username", loginAccount).or().eq("phone", loginAccount).or().eq("id_card", loginAccount));
        }
        catch (Exception e){
            log.error("fail to query account by loginAccount:{},error:{}",loginAccount,e.getMessage());
            loginResponse.setCode(ToBCodeEnum.FAIL);
            loginResponse.setMessage(e.getMessage());
            return loginResponse;
        }

        if (account == null) {
            log.info("fail to login, account: {}", loginAccount);
            loginResponse.setCode(ToBCodeEnum.FAIL);
            loginResponse.setMessage("account is null");
            return loginResponse;
        }
        // 校验密码
        String targetPassword = account.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(password, targetPassword)) {
            log.info("password is wrong, account: {}", loginAccount);
            return loginResponse;
        }
        // 登录信息封装
        loginResponse.setData(new LoginResponse.LoginUserDTO(
                account.getAccountId(),
                account.getUsername(),
                account.getNickname(),
                account.getEmail(),
                account.getGender(),
                account.getAvatarUrl(),
                account.getAddress(),
                account.getBirthday(),
                account.getIdCard(),
                account.getExt()
        ));
        loginResponse.setCode(ToBCodeEnum.SUCCESS);
        log.info("loginResponse: {}", loginResponse);
        return loginResponse;
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        Account account = new Account();
        RegisterResponse registerResponse = new RegisterResponse();
        // 赋值基础信息
        account.setUsername(registerRequest.getUsername());
        account.setNickname(registerRequest.getNickname());
        account.setPhone(registerRequest.getPhone());
        account.setAddress(registerRequest.getAddress());
        account.setBirthday(registerRequest.getBirthday());
        account.setGender(registerRequest.getGender());
        account.setIdCard(registerRequest.getIdCard());
        // 加密密码
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
        account.setPassword(encodedPassword);
        // 防重校验
        try{
            this.baseMapper.insert(account);
        }catch (DuplicateKeyException e){
            log.error("fail to register account, repeated account: {}", account);
            registerResponse.setCode(ToBCodeEnum.FAIL);
            registerResponse.setMessage(e.getMessage());
            return registerResponse;
        }catch (Exception e){
            log.error("fail to register account, db wrong: {}", account);
            registerResponse.setCode(ToBCodeEnum.FAIL);
            registerResponse.setMessage(e.getMessage());
            return registerResponse;
        }
        registerResponse.setCode(ToBCodeEnum.SUCCESS);
        return registerResponse;
    }

    @Override
    public GetUserInfoResponse getUserInfo(GetUserInfoRequest getUserInfoRequest) {
        GetUserInfoResponse getUserInfoResponse = new GetUserInfoResponse();
        String join = StrUtil.join(",", getUserInfoRequest.getUserIds());
        List<GetUserInfoResponse.UserInfoDTO> userInfoDTOList;
        try {
             userInfoDTOList = lambdaQuery()
                    .in(Account::getAccountId, getUserInfoRequest)
                    .last("order by field(id, " + join + ")")
                    .list()
                    .stream().map(account ->
                            BeanUtil.copyProperties(account, GetUserInfoResponse.UserInfoDTO.class))
                    .collect(Collectors.toList());
        }catch (Exception e){
            log.error("fail to get user info, error:{}",e.getMessage());
            getUserInfoResponse.setCode(ToBCodeEnum.FAIL);
            getUserInfoResponse.setMessage(e.getMessage());
            return getUserInfoResponse;
        }
        getUserInfoResponse.setCode(ToBCodeEnum.SUCCESS);
        getUserInfoResponse.setUsers(userInfoDTOList);
        return getUserInfoResponse;
    }

    @Override
    public UpdateUserInfoResponse updateUserInfo(UpdateUserInfoRequest updateUserInfoRequest) {
        Long accountId = Long.valueOf(updateUserInfoRequest.getUpdateData().get("account_id").toString());
        Account account = new Account();

        UpdateUserInfoResponse updateUserInfoResponse = new UpdateUserInfoResponse();
        if(accountUtils.validateUpdateData(updateUserInfoRequest.getUpdateData())){
            log.error("illegal update data,{}", updateUserInfoRequest.getUpdateData());
            updateUserInfoResponse.setCode(ToBCodeEnum.FAIL);
            updateUserInfoResponse.setMessage("illegal update data");
            return updateUserInfoResponse;
        }
        try {
            account = this.baseMapper.selectById(accountId);
        }catch (Exception e){
            log.error("fail to get account, error:{}",e.getMessage());
            updateUserInfoResponse.setCode(ToBCodeEnum.FAIL);
            updateUserInfoResponse.setMessage(e.getMessage());
            return updateUserInfoResponse;
        }

        if (account == null) {
            log.error("fail to update user info, account is null");
            updateUserInfoResponse.setCode(ToBCodeEnum.FAIL);
            updateUserInfoResponse.setMessage("account is null");
            return updateUserInfoResponse;
        }
        UpdateWrapper<Account> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("account_id", accountId);
        updateUserInfoRequest.getUpdateData().forEach((key,value) -> {
            if(value != null) {
                updateWrapper.set(key, value);
            }
        });
        updateWrapper.set("update_time", new Date());

        try{
            boolean check = update(updateWrapper);
            if (!check) {
                updateUserInfoResponse.setCode(ToBCodeEnum.FAIL);
                updateUserInfoResponse.setMessage("account update failed");
                return updateUserInfoResponse;
            }
        }catch (Exception e){
            log.error("fail to update user info, error:{}",e.getMessage());
            updateUserInfoResponse.setCode(ToBCodeEnum.FAIL);
            updateUserInfoResponse.setMessage(e.getMessage());
            return updateUserInfoResponse;
        }
        updateUserInfoResponse.setCode(ToBCodeEnum.SUCCESS);
        return updateUserInfoResponse;
    }

    @Override
    public UpdatePasswordResponse updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        Long userId = updatePasswordRequest.getUserId();
        String oldPassword = updatePasswordRequest.getOldPassword();
        Account account = new Account();
        UpdatePasswordResponse updatePasswordResponse = new UpdatePasswordResponse();
        try {
            account = this.baseMapper.selectById(userId);
            if (!oldPassword.equals(account.getPassword())) {
                log.error("fail to update password, old password is wrong");
                updatePasswordResponse.setCode(ToBCodeEnum.FAIL);
                updatePasswordResponse.setMessage("old password is wrong");
                return updatePasswordResponse;
            }
            account.setPassword(oldPassword);
            this.baseMapper.updateById(account);
        }catch (Exception e){
            log.error("fail to update password, error:{}",e.getMessage());
            updatePasswordResponse.setCode(ToBCodeEnum.FAIL);
            updatePasswordResponse.setMessage(e.getMessage());
            return updatePasswordResponse;
        }
        updatePasswordResponse.setCode(ToBCodeEnum.SUCCESS);
        return updatePasswordResponse;
    }

    @Override
    public DeleteUserResponse deleteUser(DeleteUserRequest deleteUserRequest) {
        DeleteUserResponse deleteUserResponse = new DeleteUserResponse();
        try {
            removeByIds(Arrays.asList(deleteUserRequest.getUserId()));
        }catch (Exception e){
            log.error("fail to delete user, error:{}",e.getMessage());
            deleteUserResponse.setCode(ToBCodeEnum.FAIL);
            deleteUserResponse.setMessage(e.getMessage());
            return deleteUserResponse;
        }
        deleteUserResponse.setCode(ToBCodeEnum.SUCCESS);
        return deleteUserResponse;
    }
}

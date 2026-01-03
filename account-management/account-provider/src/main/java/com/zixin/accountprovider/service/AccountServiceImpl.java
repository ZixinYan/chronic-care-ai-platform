package com.zixin.accountprovider.service;

import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.accountapi.dto.*;
import com.zixin.utils.utils.Result;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class AccountServiceImpl implements AccountManagementAPI {
    @Override
    public Result<LogInResponse> logIn(LogInRequest logInRequest) {
        return null;
    }

    @Override
    public Result<RegisterResponse> register(RegisterRequest registerRequest) {
        return null;
    }

    @Override
    public Result<GetUserInfoResponse> getUserInfo(GetUserInfoRequest getUserInfoRequest) {
        return null;
    }

    @Override
    public Result<UpdateUserInfoResponse> updateUserInfo(UpdateUserInfoRequest updateUserInfoRequest) {
        return null;
    }

    @Override
    public Result<UpdatePasswordResponse> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        return null;
    }

    @Override
    public Result<UpdateEmailResponse> updateEmail(UpdateEmailRequest updateEmailRequest) {
        return null;
    }

    @Override
    public Result<UpdatePhoneResponse> updatePhone(UpdatePhoneRequest updatePhoneRequest) {
        return null;
    }

    @Override
    public Result<DeleteUserResponse> deleteUser(DeleteUserRequest deleteUserRequest) {
        return null;
    }
}

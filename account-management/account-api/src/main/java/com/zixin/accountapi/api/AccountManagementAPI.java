package com.zixin.accountapi.api;

import com.zixin.accountapi.dto.*;
import com.zixin.utils.utils.Result;

public interface AccountManagementAPI {
    /**
     * 用户登录
     * @param logInRequest
     * @return
     */
    Result<LogInResponse> logIn(LogInRequest logInRequest);
    /**
     * 用户注册
     * @param registerRequest
     * @return
     */
    Result<RegisterResponse> register(RegisterRequest registerRequest);
    /**
     * 批量或者单个获取用户信息
     * @param getUserInfoRequest
     * @return
     */
    Result<GetUserInfoResponse> getUserInfo(GetUserInfoRequest getUserInfoRequest);
    /**
     * 更新用户基本信息
     * @param updateUserInfoRequest
     * @return
     */
    Result<UpdateUserInfoResponse> updateUserInfo(UpdateUserInfoRequest updateUserInfoRequest);
    Result<UpdatePasswordResponse> updatePassword(UpdatePasswordRequest updatePasswordRequest);
    Result<UpdateEmailResponse> updateEmail(UpdateEmailRequest updateEmailRequest);
    Result<UpdatePhoneResponse> updatePhone(UpdatePhoneRequest updatePhoneRequest);
    /**
     * 删除用户
     * @param deleteUserRequest
     * @return
     */
    Result<DeleteUserResponse> deleteUser(DeleteUserRequest deleteUserRequest);
}


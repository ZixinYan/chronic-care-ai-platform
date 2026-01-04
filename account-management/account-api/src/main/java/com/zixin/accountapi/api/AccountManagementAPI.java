package com.zixin.accountapi.api;

import com.zixin.accountapi.dto.*;

public interface AccountManagementAPI {
    /**
     * 用户登录
     * @param logInRequest
     * @return
     */
    LoginResponse login(LoginRequest logInRequest);
    /**
     * 用户注册
     * @param registerRequest
     * @return
     */
    RegisterResponse register(RegisterRequest registerRequest);
    /**
     * 批量或者单个获取用户信息
     * @param getUserInfoRequest
     * @return
     */
    GetUserInfoResponse getUserInfo(GetUserInfoRequest getUserInfoRequest);
    /**
     * 更新用户基本信息
     * @param updateUserInfoRequest
     * @return
     */
    UpdateUserInfoResponse updateUserInfo(UpdateUserInfoRequest updateUserInfoRequest);
    UpdatePasswordResponse updatePassword(UpdatePasswordRequest updatePasswordRequest);
    UpdateEmailResponse updateEmail(UpdateEmailRequest updateEmailRequest);
    UpdatePhoneResponse updatePhone(UpdatePhoneRequest updatePhoneRequest);
    /**
     * 删除用户
     * @param deleteUserRequest
     * @return
     */
    DeleteUserResponse deleteUser(DeleteUserRequest deleteUserRequest);
}


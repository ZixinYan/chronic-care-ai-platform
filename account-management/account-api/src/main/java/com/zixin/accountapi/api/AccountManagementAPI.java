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
    /**
     * 删除用户
     * @param deleteUserRequest
     * @return
     */
    DeleteUserResponse deleteUser(DeleteUserRequest deleteUserRequest);
    /**
     * 更新用户角色
     * @param request 更新角色请求
     * @return 更新结果
     */
    UpdateUserRolesResponse updateUserRoles(UpdateUserRolesRequest request);
    /**
     * 分页获取用户列表（管理后台使用）
     * @param request 查询请求
     * @return 用户列表
     */
    GetUsersListResponse getUsersList(GetUsersListRequest request);
    /**
     * 获取系统统计数据（管理后台使用）
     * @return 系统统计数据
     */
    SystemStatsResponse getSystemStats();
}


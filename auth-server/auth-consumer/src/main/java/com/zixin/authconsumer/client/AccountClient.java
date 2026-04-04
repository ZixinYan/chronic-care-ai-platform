package com.zixin.authconsumer.client;

import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.accountapi.dto.*;
import com.zixin.accountapi.enums.RoleCode;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AccountClient {
    @DubboReference(timeout = 50000)
    private AccountManagementAPI accountManagementAPI;

    public LoginResponse login(LoginRequest loginRequest) {
        return accountManagementAPI.login(loginRequest);
    }

    public RegisterResponse register(RegisterRequest registerRequest) {
        return accountManagementAPI.register(registerRequest);
    }

    public boolean updateUserInfo(UpdateUserInfoRequest request) {
        UpdateUserInfoResponse response = accountManagementAPI.updateUserInfo(request);
        if(response.getCode().equals(ToBCodeEnum.SUCCESS)){
            return true;
        }else{
            log.error("Failed to update user info, code: {}, message: {}", response.getCode(), response.getMessage());
            return false;
        }
    }

    public GetUserInfoResponse.UserInfoDTO getUserInfo(Long userId) {
        GetUserInfoRequest request = new GetUserInfoRequest();
        request.setUserIds(Collections.singletonList(userId));
        GetUserInfoResponse response = accountManagementAPI.getUserInfo(request);
        if(response.getCode().equals(ToBCodeEnum.SUCCESS) && response.getUsers() != null && !response.getUsers().isEmpty()){
            GetUserInfoResponse.UserInfoDTO userInfo = response.getUsers().get(0);
            log.info("getUserInfo from account service - userId: {}, roles: {}, permissions: {}", 
                    userId, userInfo.getRoles(), userInfo.getPermissions());
            return userInfo;
        }
        log.error("Failed to get user info, code: {}, message: {}", response.getCode(), response.getMessage());
        return null;
    }

    public GetUsersListResponse getUsersList(GetUsersListRequest request) {
        return accountManagementAPI.getUsersList(request);
    }

    public UpdateUserRolesResponse updateUserRoles(UpdateUserRolesRequest request) {
        return accountManagementAPI.updateUserRoles(request);
    }

    public DeleteUserResponse deleteUsers(DeleteUserRequest request) {
        return accountManagementAPI.deleteUser(request);
    }

    public List<Map<String, Object>> getAllRoles() {
        List<Map<String, Object>> roles = new ArrayList<>();
        for (RoleCode roleCode : RoleCode.values()) {
            Map<String, Object> role = new HashMap<>();
            role.put("code", roleCode.getCode());
            role.put("name", roleCode.getDesc());
            roles.add(role);
        }
        return roles;
    }

    public SystemStatsResponse getSystemStats() {
        return accountManagementAPI.getSystemStats();
    }
}

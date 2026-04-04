package com.zixin.authconsumer.controller;

import com.zixin.accountapi.dto.*;
import com.zixin.authconsumer.client.AccountClient;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.security.RequireRole;
import com.zixin.utils.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AccountClient accountClient;

    @GetMapping("/users")
    @RequireRole("ADMIN")
    public Result<?> getUsersList(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "roleCode", required = false) Integer roleCode) {
        
        log.info("Admin get users list request - pageNum: {}, pageSize: {}, keyword: {}, roleCode: {}", 
                pageNum, pageSize, keyword, roleCode);
        
        GetUsersListRequest request = new GetUsersListRequest();
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);
        request.setRoleCode(roleCode);
        
        GetUsersListResponse response = accountClient.getUsersList(request);
        
        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }

    @PutMapping("/users/{userId}/roles")
    @RequireRole("ADMIN")
    public Result<?> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody List<Integer> roleCodes) {
        
        log.info("Admin update user roles request - userId: {}, roleCodes: {}", userId, roleCodes);
        
        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setUserId(userId);
        request.setRoleCodes(roleCodes);
        
        UpdateUserRolesResponse response = accountClient.updateUserRoles(request);
        
        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }

    @DeleteMapping("/users")
    @RequireRole("ADMIN")
    public Result<?> deleteUsers(@RequestBody Long[] userIds) {
        
        log.info("Admin delete users request - userIds: {}", Arrays.toString(userIds));
        
        DeleteUserRequest request = new DeleteUserRequest();
        request.setUserId(userIds);
        
        DeleteUserResponse response = accountClient.deleteUsers(request);
        
        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }

    @GetMapping("/roles")
    @RequireRole("ADMIN")
    public Result<?> getAllRoles() {
        log.info("Admin get all roles request");
        return Result.success(accountClient.getAllRoles());
    }

    @GetMapping("/stats")
    @RequireRole("ADMIN")
    public Result<?> getSystemStats() {
        log.info("Admin get system stats request");
        SystemStatsResponse response = accountClient.getSystemStats();
        
        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }
}

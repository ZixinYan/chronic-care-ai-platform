package com.zixin.authconsumer.controller;

import com.zixin.accountapi.dto.GetUserInfoResponse;
import com.zixin.accountapi.dto.LoginRequest;
import com.zixin.accountapi.dto.RegisterRequest;
import com.zixin.accountapi.dto.UpdateUserInfoRequest;
import com.zixin.accountapi.dto.UpdateUserInfoResponse;
import com.zixin.authconsumer.client.AccountClient;
import com.zixin.authconsumer.client.AuthClient;
import com.zixin.authconsumer.client.ThirdPartyClient;
import com.zixin.authconsumer.service.SSOServiceImpl;
import com.zixin.authconsumer.utils.FileUtils;
import com.zixin.thirdpartyapi.dto.OSSUploadFileRequest;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.Result;
import dto.RefreshTokenRequest;
import dto.RefreshTokenResponse;
import dto.ValidateTokenRequest;
import dto.ValidateTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.google.common.io.Files.getFileExtension;
import static com.zixin.authconsumer.utils.SMSUtils.generateRandomCode;


/**
 * SSO认证控制器
 * 提供登录、注册、Token刷新、Token验证等接口
 * 
 * 注意: 这些接口应该配置在Gateway的白名单中，不需要Token验证
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SSOController {

    private final SSOServiceImpl ssoService;
    private final AuthClient authClient;
    private final ThirdPartyClient thirdPartyClient;
    private final AccountClient accountClient;

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求(包含账号和密码)
     * @return 包含Token的登录结果
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login request received for account: {}", loginRequest.getLoginAccount());
        return ssoService.login(loginRequest);
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest registerRequest) {
        log.info("Register request received for account: {}", registerRequest.getUsername());
        return ssoService.register(registerRequest);
    }

    /**
     * 发送短信验证码
     * 
     * @param sendSMSRequest 短信发送请求
     * @return 发送结果
     */
    @PostMapping("/sms/code")
    public Result<?> sendSmsCode(@RequestBody SendSMSRequest sendSMSRequest) {
        log.info("SMS code request received for phone: {}", sendSMSRequest.getPhone());
        return ssoService.sendSmsCode(sendSMSRequest.getPhone());
    }

    /**
     * 刷新Token
     * 使用Refresh Token获取新的Token对
     * 
     * @param request 包含Refresh Token的请求
     * @return 新的Token对
     */
    @PostMapping("/refresh")
    public Result<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        
        RefreshTokenResponse response = authClient.refreshToken(request);
        
        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            log.warn("Token refresh failed: {}", response.getMessage());
            return Result.error(response.getMessage());
        }
    }

    /**
     * 验证Token
     * 验证Token是否有效，并返回用户信息
     * 
     * @param request 包含Token的请求
     * @return 验证结果和用户信息
     */
    @PostMapping("/validate")
    public Result<?> validateToken(@RequestBody ValidateTokenRequest request) {
        log.info("Token validation request received");
        
        ValidateTokenResponse response = authClient.validateToken(request);
        
        if (response.getValid()) {
            return Result.success(response);
        } else {
            log.warn("Token validation failed: {}", response.getMessage());
            return Result.error(response.getMessage());
        }
    }

    /**
     * 登出
     * 撤销Token，将其加入黑名单
     * 
     * @param request 包含Token的请求
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<?> logout(@RequestBody ValidateTokenRequest request) {
        log.info("Logout request received");
        
        ValidateTokenResponse response = authClient.revokeToken(request);
        
        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success("Logout successful");
        } else {
            log.warn("Logout failed: {}", response.getMessage());
            return Result.error(response.getMessage());
        }
    }

    @PostMapping("/update-user")
    public Result<?> updateUserInfo(@RequestBody UpdateUserInfoRequest updateRequest) {
        log.info("Update user info request received for account: {}", updateRequest.getUpdateData());
        if(updateRequest.getUpdateData().get("password") != null) {
            log.warn("Attempt to update password through update user info endpoint");
            return Result.error("Password updates are not allowed through this endpoint");
        }
        if(accountClient.updateUserInfo(updateRequest)){
            return Result.success();
        }else{
            log.warn("Failed to update user info for account: {}", updateRequest.getUpdateData());
            return Result.error("Failed to update user info");
        }
    }

    @GetMapping("/user-info")
    public Result<?> getCurrentUserInfo() {
        Long userId = UserInfoManager.getUserId();
        log.info("Get current user info request, userId: {}", userId);
        GetUserInfoResponse.UserInfoDTO userInfo = accountClient.getUserInfo(userId);
        if (userInfo != null) {
            return Result.success(userInfo);
        } else {
            log.warn("Failed to get user info for userId: {}", userId);
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadAvatar(@RequestParam(value = "file") MultipartFile file) {
        Result<String> validationResult = FileUtils.validateImageFile(file);
        if (validationResult != null) {
            return validationResult;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(Objects.requireNonNull(originalFilename));
            String newFileName = String.format("%d_%s.%s",
                    System.currentTimeMillis(),
                    UUID.randomUUID().toString().replace("-", ""),
                    extension);

            OSSUploadFileRequest request = new OSSUploadFileRequest();
            request.setFileName(newFileName);
            request.setFile(file.getBytes());

            String fileUrl = thirdPartyClient.uploadFile(request);

            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("Failed to upload avatar file: {}", originalFilename);
                return Result.error("头像上传失败");
            }

            Long userId = UserInfoManager.getUserId();
            UpdateUserInfoRequest updateRequest = new UpdateUserInfoRequest();
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("avatar", fileUrl);
            updateRequest.setUpdateData(updateData);

            if (!accountClient.updateUserInfo(updateRequest)) {
                log.warn("Failed to update avatar in database for user: {}", userId);
                return Result.error("头像保存失败");
            }

            log.info("Avatar uploaded and saved successfully: {} -> {}, userId: {}", originalFilename, fileUrl, userId);
            return Result.success(fileUrl);

        } catch (IOException e) {
            log.error("IO error while uploading avatar: {}", e.getMessage(), e);
            return Result.error("文件读取失败");
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage(), e);
            return Result.error("头像上传失败");
        }
    }


    @PostMapping("/forgot-password")
    public Result<?> forgotPassword(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String phone = request.get("phone");
        if (Objects.isNull(phone) || phone.isEmpty()) {
            log.warn("Forgot password request missing phone number");
            return Result.error("Phone number is required");
        }

        if(!ssoService.verifySmsCode(phone, code)) {
            log.warn("Invalid SMS code for phone: {}", phone);
            return Result.error("Invalid SMS code");
        }

        String newPassword = request.get("newPassword");
        if (Objects.isNull(newPassword) || newPassword.isEmpty()) {
            log.warn("Forgot password request missing new password for phone: {}", phone);
            return Result.error("New password is required");
        }
        UpdateUserInfoRequest updateData = (UpdateUserInfoRequest) Map.of(
                "password", newPassword,
                "phone", phone);
        if(!accountClient.updateUserInfo(updateData)){
            log.error("Failed to update password for phone: {}", phone);
            return Result.error("Failed to update password");
        }
        log.info("Password updated successfully for phone: {}", phone);
        return Result.success();
    }
}

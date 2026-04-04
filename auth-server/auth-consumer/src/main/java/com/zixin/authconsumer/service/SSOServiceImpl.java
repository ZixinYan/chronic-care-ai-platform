package com.zixin.authconsumer.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zixin.accountapi.dto.*;
import com.zixin.authapi.api.LoginWithPhoneAPI;
import com.zixin.authconsumer.client.AccountClient;
import com.zixin.authconsumer.client.AuthClient;
import com.zixin.authconsumer.client.ThirdPartyClient;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.Result;
import dto.GenTokenRequest;
import dto.GenTokenResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.zixin.authconsumer.utils.SMSUtils.generateRandomCode;

/**
 * SSO单点登录服务实现
 * 整合账户服务和认证服务，提供统一的登录接口
 */
@Service
@Slf4j
public class SSOServiceImpl implements LoginWithPhoneAPI {

    // 验证码缓存：手机号 -> 验证码，5分钟过期
    private Cache<String, String> smsCodeCache;

    // 发送次数缓存：手机号 -> 发送次数，1小时过期
    private Cache<String, Integer> sendCountCache;

    // 最近发送时间缓存：手机号 -> 发送时间戳，用于60秒内防刷
    private Cache<String, Long> lastSendTimeCache;

    @Value("${sms.code.max-attempts:5}")
    private int maxAttemptsPerHour;

    @Value("${sms.code.length:6}")
    private int codeLength;

    @Value("${sms.code.resend-interval:60}")
    private int resendInterval;

    private final ThirdPartyClient thirdPartyClient;
    private final AccountClient accountClient;
    private final AuthClient authClient;

    public SSOServiceImpl(ThirdPartyClient thirdPartyClient,
                          AccountClient accountClient,
                          AuthClient authClient) {
        this.thirdPartyClient = thirdPartyClient;
        this.accountClient = accountClient;
        this.authClient = authClient;
    }

    @PostConstruct
    public void init() {
        // 初始化验证码缓存，5分钟过期
        smsCodeCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // 初始化发送次数缓存，1小时过期
        sendCountCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10000)
                .build();

        // 初始化最近发送时间缓存，60秒过期
        lastSendTimeCache = CacheBuilder.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
    }

    @Override
    public Result<?> login(LoginRequest loginRequest) {
        // 1. 校验参数
        if (loginRequest == null
                || loginRequest.getLoginAccount() == null
                || loginRequest.getLoginAccount().isEmpty()
                || loginRequest.getPassword() == null
                || loginRequest.getPassword().isEmpty()) {
            log.warn("Invalid login request parameters");
            return Result.error("Invalid login request");
        }

        try {
            // 2. 调用账号服务进行登录验证
            LoginResponse loginResponse = accountClient.login(loginRequest);
            if (loginResponse.getCode() != ToBCodeEnum.SUCCESS) {
                log.warn("Account login failed: {}", loginResponse.getMessage());
                return Result.error(loginResponse.getMessage());
            }

            LoginResponse.LoginUserDTO loginUserDTO = loginResponse.getData();

            Long userId = loginUserDTO.getUserId();
            String username = loginUserDTO.getUsername();
            List<String> roleNames = loginUserDTO.getRole();       // 角色名称列表
            Set<String> permissions = loginUserDTO.getPermission();    // 权限code集合

            if (userId == null || username == null) {
                log.error("Login response invalid: userId or username is null");
                return Result.error("Login response invalid");
            }

            // 4. 生成双Token (Access Token + Refresh Token)
            GenTokenRequest genTokenRequest = new GenTokenRequest();
            genTokenRequest.setUserId(userId);
            genTokenRequest.setUsername(username);
            genTokenRequest.setRoles(roleNames);
            genTokenRequest.setPermissions(permissions);

            GenTokenResponse tokenResponse = authClient.generateToken(genTokenRequest);
            if (tokenResponse.getCode() != ToBCodeEnum.SUCCESS) {
                log.error("Failed to generate JWT: {}", tokenResponse.getMessage());
                return Result.error("Failed to generate JWT");
            }

            // 验证Token生成结果
            if (tokenResponse.getAccessToken() == null || tokenResponse.getRefreshToken() == null) {
                log.error("Token generation returned null tokens");
                return Result.error("Failed to generate JWT tokens");
            }

            // 5. 构建返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("username", username);
            data.put("nickname", loginUserDTO.getNickname());
            data.put("avatarUrl", loginUserDTO.getAvatarUrl());
            data.put("phone", loginUserDTO.getPhone());
            data.put("email", loginUserDTO.getEmail());
            data.put("gender", loginUserDTO.getGender());
            data.put("address", loginUserDTO.getAddress());
            data.put("birthday", loginUserDTO.getBirthday());
            data.put("accessToken", tokenResponse.getAccessToken());
            data.put("refreshToken", tokenResponse.getRefreshToken());
            data.put("tokenType", tokenResponse.getTokenType());
            data.put("role", roleNames);
            data.put("permission", new ArrayList<>(permissions));

            log.info("User logged in successfully - userId: {}, username: {}, role name:{}, permission:{}",
                    userId, username, roleNames, permissions);
            return Result.success(data);

        } catch (Exception e) {
            log.error("Login failed with exception", e);
            return Result.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * 将角色code转换为角色名称
     * 使用RoleCode枚举进行转换
     *
     * @param roleCodes 角色code列表 (1-DOCTOR, 2-PATIENT, 3-FAMILY)
     * @return 角色名称列表 (["DOCTOR", "PATIENT", "FAMILY"])
     */
    private List<String> convertRoleCodesToNames(List<Integer> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }

        return roleCodes.stream()
                .map(code -> {
                    try {
                        // 使用RoleCode枚举进行转换
                        return com.zixin.accountapi.enums.RoleCode.fromCode(code).name();
                    } catch (Exception e) {
                        log.warn("Unknown role code: {}, fallback to USER", code);
                        return "USER";
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result<?> register(RegisterRequest registerRequest) {
        // 1. 调用account服务进行注册
        RegisterResponse response;
        try {
            response = accountClient.register(registerRequest);
            if (response.getCode() != ToBCodeEnum.SUCCESS) {
                log.error("Account registration failed: {}", response.getMessage());
                return Result.error(response.getMessage());
            }
        } catch (Exception e) {
            log.error("Account registration failed with exception", e);
            return Result.error("Account registration failed: " + e.getMessage());
        }
        return Result.success().setData(response);
    }

    /**
     * 发送短信验证码
     */
    public Result<String> sendSmsCode(String phone) {
        log.info("SMS code request received for phone: {}", phone);

        // 1. 验证手机号
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error("手机号不能为空");
        }

        // 2. 检查60秒内是否重复发送
        Long lastSendTime = lastSendTimeCache.getIfPresent(phone);
        if (lastSendTime != null) {
            long secondsSinceLastSend = (System.currentTimeMillis() - lastSendTime) / 1000;
            if (secondsSinceLastSend < resendInterval) {
                log.warn("Phone: {} requested too frequently, {} seconds since last send",
                        phone, secondsSinceLastSend);
                return Result.error("发送太频繁，请" + (resendInterval - secondsSinceLastSend) + "秒后再试");
            }
        }

        // 3. 检查1小时内发送次数限制
        Integer sendCount = sendCountCache.getIfPresent(phone);
        if (sendCount != null && sendCount >= maxAttemptsPerHour) {
            log.warn("Phone: {} has exceeded max attempts per hour", phone);
            return Result.error("发送次数已达上限，请1小时后再试");
        }

        // 4. 生成随机验证码
        String code = generateRandomCode(codeLength);
        log.info("Generated SMS code: {} for phone: {}", code, phone);

        // 5. 发送短信
        SendSMSRequest smsRequest = new SendSMSRequest();
        smsRequest.setPhone(phone);
        smsRequest.setCode(code);

        boolean sent = thirdPartyClient.sendSMS(smsRequest);

        if (sent) {
            // 6. 存储验证码到缓存
            smsCodeCache.put(phone, code);

            // 7. 更新发送次数
            int newCount = (sendCount == null) ? 1 : sendCount + 1;
            sendCountCache.put(phone, newCount);

            // 8. 记录最后发送时间
            lastSendTimeCache.put(phone, System.currentTimeMillis());

            log.info("SMS code sent successfully to phone: {}, current hour count: {}",
                    phone, newCount);

            return Result.success();
        } else {
            log.error("Failed to send SMS to phone: {}", phone);
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 验证短信验证码
     */
    public boolean verifySmsCode(String phone, String inputCode) {
        String storedCode = smsCodeCache.getIfPresent(phone);

        if (storedCode == null) {
            log.warn("No SMS code found for phone: {} or code expired", phone);
            return false;
        }

        boolean isValid = storedCode.equals(inputCode);

        if (isValid) {
            // 验证成功后删除验证码（防止重复使用）
            smsCodeCache.invalidate(phone);
            log.info("SMS code verified successfully for phone: {}", phone);
        } else {
            log.warn("Invalid SMS code for phone: {}", phone);
        }

        return isValid;
    }
}

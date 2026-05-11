package com.zixin.authapi.api;

import com.zixin.accountapi.dto.LoginRequest;
import com.zixin.accountapi.dto.RegisterRequest;
import com.zixin.accountapi.dto.UpdateUserInfoRequest;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.utils.utils.Result;

public interface LoginWithPhoneAPI {
    /**
     * Login with phone number
     * @param loginRequest
     * @return
     */
    Result login(LoginRequest loginRequest);
    /**
     * Login with phone number and SMS verification code
     * @param phone 手机号
     * @param code  短信验证码
     * @return 登录结果(包含Token)
     */
    Result loginByPhone(String phone, String code);
    /**
     * Register with phone number
     * @param registerRequest
     * @return
     */
    Result register(RegisterRequest registerRequest);
}

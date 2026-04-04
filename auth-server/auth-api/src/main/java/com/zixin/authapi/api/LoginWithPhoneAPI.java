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
     * Register with phone number
     * @param registerRequest
     * @return
     */
    Result register(RegisterRequest registerRequest);
}

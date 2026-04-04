package com.zixin.authconsumer.client;

import com.zixin.authapi.api.JwtAPI;
import com.zixin.authapi.api.TokenValidationAPI;
import dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * Auth服务Dubbo客户端
 * 通过Dubbo调用auth-provider的认证服务
 */
@Slf4j
@Service
public class AuthClient {
    
    @DubboReference(timeout = 50000)
    private JwtAPI jwtAPI;
    
    @DubboReference(timeout = 50000)
    private TokenValidationAPI tokenValidationAPI;

    /**
     * 生成Token对(Access Token + Refresh Token)
     *
     * @param request 包含用户ID和角色信息的请求
     * @return Token响应
     */
    public GenTokenResponse generateToken(GenTokenRequest request) {
        try {
            log.debug("Generating token for userId: {}", request.getUserId());
            return jwtAPI.genToken(request);
        } catch (Exception e) {
            log.error("Failed to generate token", e);
            GenTokenResponse response = new GenTokenResponse();
            response.setCode(com.zixin.utils.exception.ToBCodeEnum.FAIL);
            response.setMessage("Token generation failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * 刷新Token
     *
     * @param request 包含Refresh Token的请求
     * @return 新的Token对
     */
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            log.debug("Refreshing token");
            return jwtAPI.refreshToken(request);
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            RefreshTokenResponse response = new RefreshTokenResponse();
            response.setCode(com.zixin.utils.exception.ToBCodeEnum.FAIL);
            response.setMessage("Token refresh failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * 验证Token
     *
     * @param request 包含Token的请求
     * @return 验证结果
     */
    public ValidateTokenResponse validateToken(ValidateTokenRequest request) {
        try {
            log.debug("Validating token");
            return tokenValidationAPI.validateToken(request);
        } catch (Exception e) {
            log.error("Failed to validate token", e);
            ValidateTokenResponse response = new ValidateTokenResponse();
            response.setCode(com.zixin.utils.exception.ToBCodeEnum.FAIL);
            response.setMessage("Token validation failed: " + e.getMessage());
            response.setValid(false);
            return response;
        }
    }

    /**
     * 撤销Token(登出)
     *
     * @param request 包含Token的请求
     * @return 操作结果
     */
    public ValidateTokenResponse revokeToken(ValidateTokenRequest request) {
        try {
            log.debug("Revoking token");
            return tokenValidationAPI.revokeToken(request);
        } catch (Exception e) {
            log.error("Failed to revoke token", e);
            ValidateTokenResponse response = new ValidateTokenResponse();
            response.setCode(com.zixin.utils.exception.ToBCodeEnum.FAIL);
            response.setMessage("Token revocation failed: " + e.getMessage());
            return response;
        }
    }
}

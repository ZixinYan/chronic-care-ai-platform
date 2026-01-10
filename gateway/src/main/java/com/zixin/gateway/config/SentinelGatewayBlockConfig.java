package com.zixin.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.zixin.utils.exception.ToCCodeEnum;
import com.zixin.utils.utils.Result;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class SentinelGatewayBlockConfig {

    public SentinelGatewayBlockConfig() {
        GatewayCallbackManager.setBlockHandler((exchange, throwable) -> {

            Result error = Result.error(
                    ToCCodeEnum.TOO_MANY_REQUEST.getCode(),
                    ToCCodeEnum.TOO_MANY_REQUEST.getMsg()
            );

            return ServerResponse
                    .status(429)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(error);
        });
    }
}

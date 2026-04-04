package com.zixin.utils.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "encryption.aes")
public class AesProperties {

    /**
     * 当前使用的key版本
     */
    private String current = "v2";

    /**
     * 所有key
     */
    private Map<String, String> keys;
}
package com.zixin.utils.security;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SensitiveDataEncryptConfig {

    private final AesProperties aesProperties;

    public SensitiveDataEncryptConfig(AesProperties aesProperties) {
        this.aesProperties = aesProperties;
    }

    @PostConstruct
    public void init() {
        SensitiveDataEncryptHandler.setAesProperties(aesProperties);
    }
}

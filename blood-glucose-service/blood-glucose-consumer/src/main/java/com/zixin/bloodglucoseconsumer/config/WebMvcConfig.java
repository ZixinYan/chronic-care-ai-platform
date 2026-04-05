package com.zixin.bloodglucoseconsumer.config;

import com.zixin.utils.interceptor.UserInfoExtractInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserInfoExtractInterceptor userInfoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户信息拦截器
        registry.addInterceptor(userInfoInterceptor)
                .addPathPatterns("/**");
    }

    WebMvcConfig(UserInfoExtractInterceptor userInfoInterceptor) {
        this.userInfoInterceptor = userInfoInterceptor;
    }
}

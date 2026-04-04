package com.zixin.authconsumer.utils;

import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.utils.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public final class SMSUtils {
    private final static Logger log = LoggerFactory.getLogger(SMSUtils.class);
    /**
     * 生成指定位数的随机数字验证码
     */
    public static String generateRandomCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Code length must be positive");
        }

        // 使用ThreadLocalRandom保证线程安全
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));  // 生成0-9的数字
        }

        log.info("Generated random code: {}", code);
        return code.toString();
    }
}

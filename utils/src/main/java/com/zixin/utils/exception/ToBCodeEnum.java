package com.zixin.utils.exception;

import lombok.Getter;

/**
 * 微服务内调用返回
 */
@Getter
public enum ToBCodeEnum {
    SUCCESS(0, "成功"),
    FAIL(1, "失败");

    private final int code;
    private final String message;

    ToBCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}

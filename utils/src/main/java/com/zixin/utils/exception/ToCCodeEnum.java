package com.zixin.utils.exception;

import lombok.Getter;

/**
 * 对外接口暴露枚举
 */
@Getter
public enum ToCCodeEnum {
    UNKNOWN_EXCEPTION(10000, "系统未知错误");


    private final Integer code;
    private final String msg;

    private ToCCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}


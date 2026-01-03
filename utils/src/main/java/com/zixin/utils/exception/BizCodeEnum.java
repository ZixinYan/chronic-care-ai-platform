package com.zixin.utils.exception;

import lombok.Getter;

@Getter
public enum BizCodeEnum {
    UNKNOWN_EXCEPTION(10000, "系统未知错误");


    private final Integer code;
    private final String msg;

    private BizCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}


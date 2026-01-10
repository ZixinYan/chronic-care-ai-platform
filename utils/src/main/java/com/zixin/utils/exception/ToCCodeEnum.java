package com.zixin.utils.exception;

import lombok.Getter;

/**
 * 对外接口暴露枚举
 */
@Getter
public enum ToCCodeEnum {
    UNKNOWN_EXCEPTION(10000, "系统未知错误"),
    TOO_MANY_REQUEST(9999, "服务器受不了了，过会儿再来吧");


    private final Integer code;
    private final String msg;

    private ToCCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}


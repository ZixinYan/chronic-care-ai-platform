package com.zixin.utils.constant;

import lombok.Getter;

@Getter
public enum BizCode {
    HEALTHY_REPORT_JUDGEMENT("健康报告二次校验业务", 1000, "需要医生进行二次核对，以保障报告有效性");

    private final String bizName;
    private final Integer code;
    private final String desc;

    BizCode(String bizName, Integer code, String msg) {
        this.bizName = bizName;
        this.code = code;
        this.desc = msg;
    }
}

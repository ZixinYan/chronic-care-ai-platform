package com.zixin.utils.utils;

import com.zixin.utils.exception.ToBCodeEnum;
import lombok.Data;
import lombok.Getter;

@Data
public class BaseResponse {
    private ToBCodeEnum code;
    private String message;

    public BaseResponse(ToBCodeEnum codeEnum) {
        this.code = codeEnum;
        this.message = codeEnum.getMessage();
    }

    public BaseResponse() {
    }
}

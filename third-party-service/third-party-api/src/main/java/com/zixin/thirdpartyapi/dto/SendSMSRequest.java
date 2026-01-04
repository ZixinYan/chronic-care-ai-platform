package com.zixin.thirdpartyapi.dto;

import lombok.Data;

@Data
public class SendSMSRequest {
    private String phone;
    private String code;
}

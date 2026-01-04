package com.zixin.accountapi.dto;

import lombok.Data;

@Data
public class UpdatePhoneRequest {
    private Long userId;
    private String newPhone;
}




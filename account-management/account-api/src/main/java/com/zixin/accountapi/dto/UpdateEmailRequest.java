package com.zixin.accountapi.dto;

import lombok.Data;

@Data
public class UpdateEmailRequest {
    private Long userId;
    private String email;
}

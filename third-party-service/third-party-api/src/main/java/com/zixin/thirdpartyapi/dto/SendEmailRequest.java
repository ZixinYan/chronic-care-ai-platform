package com.zixin.thirdpartyapi.dto;

import lombok.Data;

@Data
public class SendEmailRequest {
    private String to;
    private String theme;
    private String content;
}

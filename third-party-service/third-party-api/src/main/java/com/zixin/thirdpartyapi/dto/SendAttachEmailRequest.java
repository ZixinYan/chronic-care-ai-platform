package com.zixin.thirdpartyapi.dto;

import lombok.Data;

@Data
public class SendAttachEmailRequest extends SendEmailRequest{
    String fileName;
    String filePath;
}

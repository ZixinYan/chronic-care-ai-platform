package com.zixin.thirdpartyapi.api;

import com.zixin.thirdpartyapi.dto.SendAttachEmailRequest;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyapi.dto.SendEmailResponse;

public interface EmailAPI {
    /**
     * 发送邮件
     */
    SendEmailResponse sendMail(SendEmailRequest sendEmailRequest);
    SendEmailResponse sendHTMLMail(SendEmailRequest sendEmailRequest);
    SendEmailResponse sendAttachmentEmail(SendAttachEmailRequest sendAttachEmailRequest);
}

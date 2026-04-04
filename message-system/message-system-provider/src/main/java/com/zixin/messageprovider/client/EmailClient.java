package com.zixin.messageprovider.client;


import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.messageprovider.runner.EmailProducer;
import com.zixin.thirdpartyapi.api.EmailAPI;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyapi.dto.SendEmailResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailClient {
    @DubboReference(timeout = 200000)
    private EmailAPI thirdPartyEmailAPI;

    @DubboReference(timeout = 50000)
    private AccountManagementAPI accountManagementAPI;

    private final EmailProducer emailProducer;

    public EmailClient(EmailProducer emailProducer) {
        this.emailProducer = emailProducer;
    }

    public boolean sendHtmlEmail(SendEmailRequest request) {
        SendEmailResponse emailResponse = thirdPartyEmailAPI.sendHTMLMail(request);

        // 检查邮件发送结果
        if (!ToBCodeEnum.SUCCESS.equals(emailResponse.getCode())) {
            log.error("Send email failed, receiverId: {}, error: {}",
                    request.getTo(), emailResponse.getMessage());
            return false;
        }

        return true;
    }



    public boolean sendEmailByKafka(SendEmailRequest request) {
        try {
            emailProducer.sendEmailTask(request);
            return false;
        } catch (Exception e) {
            log.error("Send email task failed, receiverId: {}", request.getTo(), e);
            return true;
        }
    }
}

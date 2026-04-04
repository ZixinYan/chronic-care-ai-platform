package com.zixin.thirdpartyprovider.runner;

import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyapi.dto.SendEmailResponse;
import com.zixin.thirdpartyprovider.config.KafkaConfig;
import com.zixin.thirdpartyprovider.service.EmailServiceImpl;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final EmailServiceImpl emailService;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(
                    delay = 5000,
                    multiplier = 2
            ),
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = KafkaConfig.EMAIL_CONSUMER_TOPIC, groupId = "message-group")
    public void sendEmailTask(SendEmailRequest sendEmailRequest) {
        try {
            SendEmailResponse response = emailService.sendHTMLMail(sendEmailRequest);

            if (!ToBCodeEnum.SUCCESS.equals(response.getCode())) {
                log.error("Email send failed: {}", response.getMessage());
            } else {
                log.info("Email send success: {}", sendEmailRequest.getTo());
            }
        } catch (Exception e) {
            log.error("Send email task failed, request: {}", sendEmailRequest, e);
        }
    }

}

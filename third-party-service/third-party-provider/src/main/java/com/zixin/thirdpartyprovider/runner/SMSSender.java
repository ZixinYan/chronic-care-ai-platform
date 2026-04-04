package com.zixin.thirdpartyprovider.runner;

import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.thirdpartyprovider.config.KafkaConfig;
import com.zixin.thirdpartyprovider.service.SMSServiceImpl;
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
public class SMSSender {
    private final SMSServiceImpl smsService;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(
                    delay = 5000,
                    multiplier = 2
            ),
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = KafkaConfig.SMS_CONSUMER_TOPIC, groupId = "message-group")
    public void sendSMSTask(SendSMSRequest sendSMSRequest) {
        try {
            SendSMSResponse response = smsService.sendSMS(sendSMSRequest);

            if (!ToBCodeEnum.SUCCESS.equals(response.getCode())) {
                log.error("SMS send failed: {}", response.getMessage());
            } else {
                log.info("SMS send success: {}", sendSMSRequest.getPhone());
            }
        } catch (Exception e) {
            log.error("Send SMS task failed, request: {}", sendSMSRequest, e);
        }
    }
}

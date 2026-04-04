package com.zixin.messageprovider.runner;

import com.zixin.messageprovider.config.KafkaConfig;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SMSProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSMSTask(SendSMSRequest request) {
        kafkaTemplate.send(
                KafkaConfig.SMS_SEND_TOPIC,
                request.getPhone() + "-" + System.currentTimeMillis(),
                request
        );
    }

}
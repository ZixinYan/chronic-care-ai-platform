package com.zixin.messageprovider.runner;

import com.zixin.messageprovider.config.KafkaConfig;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EmailProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEmailTask(SendEmailRequest request) {
        kafkaTemplate.send(
                KafkaConfig.EMAIL_SEND_TOPIC,
                request.getTo() + "-" + System.currentTimeMillis(),
                request
        );
    }

}
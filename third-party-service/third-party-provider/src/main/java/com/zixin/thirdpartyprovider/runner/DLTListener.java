package com.zixin.thirdpartyprovider.runner;

import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyprovider.config.KafkaConfig;
import com.zixin.thirdpartyprovider.service.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DLTListener {

    private final EmailServiceImpl emailService;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "email-send-topic-dlt")
    public void listenDLT(SendEmailRequest request) {
        // 1 记录数据库[这个暂时没做]
        log.info("假装我做了记录数据库");
        // 2 告警
        alert(request);
        // 3 人工补偿（重新生产）
        kafkaTemplate.send(
                KafkaConfig.EMAIL_SEND_TOPIC,
                request.getTo() + "-" + System.currentTimeMillis(),
                request
        );
    }


    public void alert(SendEmailRequest request) {
        // 未来可以拓展别的告警
        SendEmailRequest alertRequest = new SendEmailRequest();
        alertRequest.setTo("769913449@qq.com");
        alertRequest.setTheme("【健康平台】您有一条消息发送失败，请注意重新生产");
        alertRequest.setContent("杂鱼大哥哥，连发短信这么简单的事情都做不好吗, zako~, zako~, 人家大发慈悲的告诉你短信内容哦：\n" + request.toString());
        emailService.sendHTMLMail(alertRequest);
    }
}
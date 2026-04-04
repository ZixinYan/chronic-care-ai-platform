package com.zixin.messageconsumer.client;

import com.zixin.messageapi.api.EmailAPI;
import com.zixin.messageapi.dto.SendMessageRequest;
import com.zixin.messageapi.dto.SendMessageResponse;
import com.zixin.messageapi.enums.MessageType;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailClient {

    @DubboReference(timeout = 50000)
    private EmailAPI emailAPI;

    public boolean sendEmail(Long senderId, Long receiverId, String senderName ,String title, String content) {
        SendMessageResponse response = emailAPI.sendEmailMessage(senderId, SendMessageRequest.builder()
                        .title(title)
                        .content(content)
                        .senderName(senderName)
                        .receiverId(receiverId)
                        .build());
        if (response.getCode().equals(ToBCodeEnum.FAIL)) {
            log.error("Failed to send email from senderId {} to receiverId {}: {}", senderId, receiverId, response.getMessage());
            return false;
        }
        return true;
    }
}

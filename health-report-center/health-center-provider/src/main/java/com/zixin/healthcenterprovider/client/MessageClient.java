package com.zixin.healthcenterprovider.client;

import com.zixin.messageapi.api.MessageAPI;
import com.zixin.messageapi.dto.SendMessageRequest;
import com.zixin.messageapi.dto.SendMessageResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class MessageClient {
    @DubboReference(timeout = 50000)
    private MessageAPI messageAPI;

    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(2);

    public void sendMessageAsync(Long userId, SendMessageRequest request) {
        CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("开始异步发送消息, userId: {}, receiverId: {}",
                        userId, request.getReceiverId());

                SendMessageResponse response = messageAPI.sendMessage(userId, request);

                if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
                    log.info("异步消息发送成功, userId: {}, receiverId: {}, messageId: {}",
                            userId, request.getReceiverId(), response.getMessageId());
                    return true;
                } else {
                    log.error("异步消息发送失败, userId: {}, receiverId: {}, error: {}",
                            userId, request.getReceiverId(), response.getMessage());
                    return false;
                }
            } catch (Exception e) {
                log.error("异步消息发送异常, userId: {}, receiverId: {}",
                        userId, request.getReceiverId(), e);
                throw new RuntimeException("消息发送异常", e);
            }
        }, messageExecutor);
    }
}

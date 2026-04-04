package com.zixin.messageprovider.service;

import com.zixin.accountapi.dto.GetUserInfoResponse;
import com.zixin.messageapi.api.EmailAPI;
import com.zixin.messageapi.dto.SendMessageRequest;
import com.zixin.messageapi.dto.SendMessageResponse;
import com.zixin.messageapi.enums.MessageType;
import com.zixin.messageprovider.client.AccountClient;
import com.zixin.messageprovider.client.EmailClient;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 邮件消息服务实现类
 * 
 * 通过Dubbo调用third-party-service发送邮件
 * 
 * @author yanzixin
 * Created on 2026-02-03
 */
@Service
@DubboService
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailAPI {


    private final MessageServiceImpl messageService;
    private final AccountClient accountClient;
    private final EmailClient emailClient;

    @Override
    public SendMessageResponse sendEmailMessage(Long senderId, SendMessageRequest request) {
        SendMessageResponse response = new SendMessageResponse();
        
        try {
            // 参数验证
            if (senderId == null || request.getReceiverId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("发送者和接收者不能为空");
                return response;
            }
            
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("邮件标题不能为空");
                return response;
            }
            
            // 从用户服务获取接收者邮箱地址
            String receiverEmail = getReceiverEmail(request.getReceiverId());
            
            if (receiverEmail == null || receiverEmail.trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("接收者邮箱地址不存在");
                return response;
            }
            
            // 调用第三方邮件服务发送邮件
            SendEmailRequest emailRequest = new SendEmailRequest();
            emailRequest.setTo(receiverEmail);
            emailRequest.setTheme(request.getTitle());
            emailRequest.setContent(request.getContent());

            // 检查邮件发送结果
            if (emailClient.sendEmailByKafka(emailRequest)) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("邮件发送失败");
                return response;
            }
            
            // 保存站内消息记录
            request.setMessageType(MessageType.SYSTEM.getCode());
            SendMessageResponse messageResponse = messageService.sendMessage(senderId, request);
            
            if (ToBCodeEnum.SUCCESS.equals(messageResponse.getCode())) {
                log.info("Send email message success, messageId: {}, senderId: {}, receiverId: {}", 
                        messageResponse.getMessageId(), senderId, request.getReceiverId());
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("邮件发送成功");
                response.setMessageId(messageResponse.getMessageId());
            } else {
                // 邮件发送成功，但站内消息保存失败
                log.warn("Email sent successfully but failed to save message record, error: {}", 
                        messageResponse.getMessage());
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("邮件发送成功(消息记录保存失败)");
            }
            
        } catch (Exception e) {
            log.error("Send email message error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("发送邮件异常: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    public SendMessageResponse broadcastEmailMessage(Long senderId, SendMessageRequest request) {
        SendMessageResponse response = new SendMessageResponse();
        
        try {
            // 参数验证
            if (senderId == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("发送者不能为空");
                return response;
            }
            
            if (request.getReceiverIds() == null || request.getReceiverIds().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("接收者列表不能为空");
                return response;
            }
            
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("邮件标题不能为空");
                return response;
            }
            
            // 批量发送邮件
            List<Long> failedReceiverIds = new ArrayList<>();
            int successCount = 0;
            
            for (Long receiverId : request.getReceiverIds()) {
                try {
                    // 获取接收者邮箱地址
                    String receiverEmail = getReceiverEmail(receiverId);
                    
                    if (receiverEmail == null || receiverEmail.trim().isEmpty()) {
                        log.warn("Receiver email not found, receiverId: {}", receiverId);
                        failedReceiverIds.add(receiverId);
                        continue;
                    }
                    
                    // 调用第三方邮件服务
                    SendEmailRequest emailRequest = new SendEmailRequest();
                    emailRequest.setTo(receiverEmail);
                    emailRequest.setTheme(request.getTitle());
                    emailRequest.setContent(request.getContent());

                    if (emailClient.sendEmailByKafka(emailRequest)) {
                        response.setCode(ToBCodeEnum.FAIL);
                        response.setMessage("邮件发送失败");
                        return response;
                    }
                } catch (Exception e) {
                    log.error("Send email to receiver error, receiverId: {}", receiverId, e);
                    failedReceiverIds.add(receiverId);
                }
            }
            
            // 保存站内消息群发记录
            request.setMessageType(MessageType.BROADCAST.getCode());
            SendMessageResponse messageResponse = messageService.broadcastMessage(senderId, request);
            
            log.info("Broadcast email message completed, total: {}, success: {}, failed: {}", 
                    request.getReceiverIds().size(), successCount, failedReceiverIds.size());
            
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage(String.format("群发邮件完成, 成功: %d, 失败: %d", 
                    successCount, failedReceiverIds.size()));
            response.setMessageId(messageResponse.getMessageId());
            
        } catch (Exception e) {
            log.error("Broadcast email message error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("群发邮件异常: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取接收者邮箱地址
     * 
     * 通过Dubbo调用account-management服务获取用户邮箱
     * 
     * @param receiverId 接收者ID
     * @return 邮箱地址
     */
    private String getReceiverEmail(Long receiverId) {
        try {
            // 调用账户管理服务获取用户信息
            List<GetUserInfoResponse.UserInfoDTO> users  = accountClient.getUserInfo(Collections.singletonList(receiverId));
            if (users == null || users.isEmpty()) {
                log.warn("User not found, receiverId: {}", receiverId);
                return null;
            }
            
            // 获取邮箱地址
            GetUserInfoResponse.UserInfoDTO userInfo = users.get(0);
            String email = userInfo.getEmail();
            
            if (email == null || email.trim().isEmpty()) {
                log.warn("User email is empty, receiverId: {}", receiverId);
                return null;
            }
            
            log.debug("Successfully retrieved email for receiverId: {}", receiverId);
            return email;
            
        } catch (Exception e) {
            log.error("Error retrieving email for receiverId: {}", receiverId, e);
            return null;
        }
    }
}

package com.zixin.messageprovider.service;

import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.accountapi.dto.GetUserInfoRequest;
import com.zixin.accountapi.dto.GetUserInfoResponse;
import com.zixin.messageapi.api.SMSAPI;
import com.zixin.messageapi.dto.SendMessageRequest;
import com.zixin.messageapi.dto.SendMessageResponse;
import com.zixin.messageapi.enums.MessageType;
import com.zixin.messageprovider.client.AccountClient;
import com.zixin.messageprovider.client.SMSClient;
import com.zixin.messageprovider.config.SMSRateLimiter;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 短信消息服务实现类 (Dubbo服务)
 * 
 * 通过Dubbo调用third-party-service发送短信
 * 
 * 功能特性：
 * - 单发短信
 * - 群发短信
 * - 防刷限流（基于Redis滑动窗口）
 * 
 * @author yanzixin
 * Created on 2026-02-03
 */
@Service
@DubboService
@Slf4j
@RequiredArgsConstructor
public class SMSServiceImpl implements SMSAPI {

    private final MessageServiceImpl messageService;
    private final SMSRateLimiter smsRateLimiter;
    private final SMSClient smsClient;
    private final AccountClient accountClient;
    
    @Value("${sms.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    public SendMessageResponse sendSMSMessage(Long senderId, SendMessageRequest request) {
        SendMessageResponse response = new SendMessageResponse();
        
        try {
            // 1. 参数验证
            if (senderId == null || request.getReceiverId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("发送者和接收者不能为空");
                return response;
            }
            
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("短信内容不能为空");
                return response;
            }

            // 2. 获取接收者手机号
            String receiverPhone = getReceiverPhone(request.getReceiverId());
            
            if (receiverPhone == null || receiverPhone.trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("接收者手机号不存在");
                return response;
            }
            
            // 3. 防刷检查
            if (rateLimitEnabled) {
                SMSRateLimiter.RateLimitResult rateLimitResult = 
                    smsRateLimiter.checkRateLimit(receiverPhone, senderId);
                
                if (!rateLimitResult.isAllowed()) {
                    log.warn("SMS rate limit exceeded, senderId: {}, receiverPhone: {}, message: {}", 
                            senderId, receiverPhone, rateLimitResult.getMessage());
                    response.setCode(ToBCodeEnum.FAIL);
                    response.setMessage(rateLimitResult.getMessage());
                    return response;
                }
            }
            
            // 4. 调用第三方短信服务发送短信
            SendSMSRequest smsRequest = new SendSMSRequest();
            smsRequest.setPhone(receiverPhone);
            smsRequest.setCode(request.getContent()); // 短信内容作为code发送
            // smsRequest.setTemplateId(); // 如果需要指定模板ID

            // 5. 检查短信发送结果
            if (!smsClient.sendSMSByKafka(smsRequest)) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("短信发送失败");
                return response;
            }
            
            // 6. 记录发送（增加防刷计数器）
            if (rateLimitEnabled) {
                smsRateLimiter.recordSending(receiverPhone, senderId);
            }
            
            // 7. 保存站内消息记录
            request.setMessageType(MessageType.SYSTEM.getCode());
            SendMessageResponse messageResponse = messageService.sendMessage(senderId, request);
            
            if (ToBCodeEnum.SUCCESS.equals(messageResponse.getCode())) {
                log.info("Send SMS message success, messageId: {}, senderId: {}, receiverId: {}", 
                        messageResponse.getMessageId(), senderId, request.getReceiverId());
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("短信发送成功");
                response.setMessageId(messageResponse.getMessageId());
            } else {
                // 短信发送成功，但站内消息保存失败
                log.warn("SMS sent successfully but failed to save message record, error: {}", 
                        messageResponse.getMessage());
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("短信发送成功(消息记录保存失败)");
            }
            
        } catch (Exception e) {
            log.error("Send SMS message error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("发送短信异常: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    public SendMessageResponse broadcastSMSMessage(Long senderId, SendMessageRequest request) {
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
            
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("短信内容不能为空");
                return response;
            }
            
            // 批量发送短信
            List<Long> failedReceiverIds = new ArrayList<>();
            List<Long> rateLimitedReceiverIds = new ArrayList<>();
            int successCount = 0;
            
            for (Long receiverId : request.getReceiverIds()) {
                try {
                    // 获取接收者手机号
                    String receiverPhone = getReceiverPhone(receiverId);
                    
                    if (receiverPhone == null || receiverPhone.trim().isEmpty()) {
                        log.warn("Receiver phone not found, receiverId: {}", receiverId);
                        failedReceiverIds.add(receiverId);
                        continue;
                    }
                    
                    // 防刷检查
                    if (rateLimitEnabled) {
                        SMSRateLimiter.RateLimitResult rateLimitResult = 
                            smsRateLimiter.checkRateLimit(receiverPhone, senderId);
                        
                        if (!rateLimitResult.isAllowed()) {
                            log.warn("SMS rate limit exceeded in broadcast, senderId: {}, receiverId: {}, receiverPhone: {}", 
                                    senderId, receiverId, receiverPhone);
                            rateLimitedReceiverIds.add(receiverId);
                            continue;
                        }
                    }
                    
                    // 调用第三方短信服务
                    SendSMSRequest smsRequest = new SendSMSRequest();
                    smsRequest.setPhone(receiverPhone);
                    smsRequest.setCode(request.getContent());

                    if (!smsClient.sendSMSByKafka(smsRequest)) {
                        log.error("Send SMS to receiver failed, receiverId: {}",
                                receiverId);
                        failedReceiverIds.add(receiverId);
                    }

                    successCount++;
                    // 记录发送
                    if (rateLimitEnabled) {
                        smsRateLimiter.recordSending(receiverPhone, senderId);
                    }
                    
                } catch (Exception e) {
                    log.error("Send SMS to receiver error, receiverId: {}", receiverId, e);
                    failedReceiverIds.add(receiverId);
                }
            }
            
            // 保存站内消息群发记录
            request.setMessageType(MessageType.BROADCAST.getCode());
            SendMessageResponse messageResponse = messageService.broadcastMessage(senderId, request);
            
            log.info("Broadcast SMS message completed, total: {}, success: {}, failed: {}, rate-limited: {}", 
                    request.getReceiverIds().size(), successCount, failedReceiverIds.size(), rateLimitedReceiverIds.size());
            
            // 构建返回消息
            String resultMessage = String.format("群发短信完成, 成功: %d, 失败: %d", 
                    successCount, failedReceiverIds.size());
            if (!rateLimitedReceiverIds.isEmpty()) {
                resultMessage += String.format(", 限流跳过: %d", rateLimitedReceiverIds.size());
            }
            
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage(resultMessage);
            response.setMessageId(messageResponse.getMessageId());
            
        } catch (Exception e) {
            log.error("Broadcast SMS message error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("群发短信异常: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取接收者手机号
     * 
     * 通过Dubbo调用account-management服务获取用户手机号
     * 
     * @param receiverId 接收者ID
     * @return 手机号
     */
    private String getReceiverPhone(Long receiverId) {
        try {
            // 获取用户列表
            List<GetUserInfoResponse.UserInfoDTO> users = accountClient.getUserInfo(Collections.singletonList(receiverId));
            if (users == null || users.isEmpty()) {
                log.warn("User not found, receiverId: {}", receiverId);
                return null;
            }
            
            // 获取手机号
            GetUserInfoResponse.UserInfoDTO userInfo = users.get(0);
            String phone = userInfo.getPhone();
            
            if (phone == null || phone.trim().isEmpty()) {
                log.warn("User phone is empty, receiverId: {}", receiverId);
                return null;
            }
            
            log.debug("Successfully retrieved phone for receiverId: {}", receiverId);
            return phone;
            
        } catch (Exception e) {
            log.error("Error retrieving phone for receiverId: {}", receiverId, e);
            return null;
        }
    }
}

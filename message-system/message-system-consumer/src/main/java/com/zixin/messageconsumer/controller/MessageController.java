package com.zixin.messageconsumer.controller;

import com.zixin.messageapi.api.MessageAPI;
import com.zixin.messageapi.dto.*;
import com.zixin.messageapi.vo.MessageVO;
import com.zixin.messageconsumer.client.EmailClient;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信C端Controller
 * 
 * 提供给前端用户的接口,使用Result作为统一返回体
 * 
 * 功能:
 * 1. 查询收件箱
 * 2. 查询发件箱
 * 3. 获取消息详情
 * 4. 标记消息已读
 * 5. 删除消息
 * 6. 获取未读数量
 */
@RestController
@RequestMapping("/message")
@Slf4j
public class MessageController {
    
    @DubboReference(check = false)
    private MessageAPI messageAPI;

    private final EmailClient emailClient;

    public MessageController(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    /**
     * 查询收件箱
     *
     * @return 消息列表
     */
    @GetMapping("/inbox")
    public Result<?> queryInbox(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "messageType", required = false) Integer messageType) {
        Long userId = UserInfoManager.getUserId();
        log.info("Query inbox request, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        
        QueryMessageRequest request = new QueryMessageRequest();
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setUnreadOnly(unreadOnly);
        request.setMessageType(messageType);
        
        QueryMessageResponse response = messageAPI.queryInbox(userId, request);
        
        if (response == null || response.getCode() == null) {
            log.error("Query inbox failed: response is null, userId: {}", userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success(response.getMessageList());
        } else {
            log.warn("Query inbox failed: {}, userId: {}", response.getMessage(), userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 查询发件箱
     *
     * @return 消息列表
     */
    @GetMapping("/sent")
    public Result<?> querySentBox(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "messageType", required = false) Integer messageType) {
        Long userId = UserInfoManager.getUserId();
        log.info("Query sent box request, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        
        QueryMessageRequest request = new QueryMessageRequest();
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setMessageType(messageType);
        
        QueryMessageResponse response = messageAPI.querySentBox(userId, request);
        
        if (response == null || response.getCode() == null) {
            log.error("Query sent box failed: response is null, userId: {}", userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success(response.getMessageList());
        } else {
            log.warn("Query sent box failed: {}, userId: {}", response.getMessage(), userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 获取消息详情
     *
     * @param messageId 消息ID
     * @return 消息详情
     */
    @GetMapping("/detail")
    public Result<MessageVO> getMessageDetail(@RequestParam("messageId") Long messageId) {
        Long userId = UserInfoManager.getUserId();
        log.info("Get message detail request, messageId: {}, userId: {}", messageId, userId);
        GetMessageDetailResponse response = messageAPI.getMessageDetail(userId, messageId);
        
        if (response == null) {
            log.error("Get message detail failed: response is null, messageId: {}, userId: {}", messageId, userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode() == null) {
            log.error("Get message detail failed: code is null, messageId: {}, userId: {}", messageId, userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success(response.getMessageVO());
        } else {
            log.warn("Get message detail failed: {}, messageId: {}, userId: {}", response.getMessage(), messageId, userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PostMapping("/read")
    public Result<Boolean> markAsRead(@RequestParam("messageId") Long messageId) {
        Long userId = UserInfoManager.getUserId();
        log.info("Mark as read request, messageId: {}, userId: {}", messageId, userId);
        MarkAsReadResponse response = messageAPI.markAsRead(userId, messageId);
        
        if (response == null || response.getCode() == null) {
            log.error("Mark as read failed: response is null, messageId: {}, userId: {}", messageId, userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success();
        } else {
            log.warn("Mark as read failed: {}, messageId: {}, userId: {}", response.getMessage(), messageId, userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 批量标记消息为已读
     *
     * @param messageIds 消息ID列表
     * @return 操作结果
     */
    @PostMapping("/batch-read")
    public Result<Boolean> batchMarkAsRead(@RequestBody List<Long> messageIds) {
        Long userId = UserInfoManager.getUserId();
        log.info("Batch mark as read request, userId: {}, count: {}", userId, messageIds.size());
        MarkAsReadResponse response = messageAPI.batchMarkAsRead(userId, messageIds);
        
        if (response == null || response.getCode() == null) {
            log.error("Batch mark as read failed: response is null, userId: {}", userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success();
        } else {
            log.warn("Batch mark as read failed: {}, userId: {}", response.getMessage(), userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 删除消息
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PostMapping("/delete")
    public Result<Boolean> deleteMessage(@RequestParam("messageId") Long messageId) {
        Long userId = UserInfoManager.getUserId();
        log.info("Delete message request, messageId: {}, userId: {}", messageId, userId);
        DeleteMessageResponse response = messageAPI.deleteMessage(userId, messageId);
        
        if (response == null || response.getCode() == null) {
            log.error("Delete message failed: response is null, messageId: {}, userId: {}", messageId, userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success();
        } else {
            log.warn("Delete message failed: {}, messageId: {}, userId: {}", response.getMessage(), messageId, userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 批量删除消息
     *
     * @param messageIds 消息ID列表
     * @return 操作结果
     */
    @PostMapping("/batch-delete")
    public Result<Boolean> batchDeleteMessage(@RequestBody List<Long> messageIds) {
        Long userId = UserInfoManager.getUserId();
        log.info("Batch delete message request, userId: {}, count: {}", userId, messageIds.size());
        DeleteMessageResponse response = messageAPI.batchDeleteMessage(userId, messageIds);
        
        if (response == null || response.getCode() == null) {
            log.error("Batch delete message failed: response is null, userId: {}", userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success();
        } else {
            log.warn("Batch delete message failed: {}, userId: {}", response.getMessage(), userId);
            return Result.error(response.getMessage());
        }
    }
    
    /**
     * 获取未读消息数量
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = UserInfoManager.getUserId();
        log.info("Get unread count request, userId: {}", userId);
        UnreadCountResponse response = messageAPI.getUnreadCount(userId);
        
        if (response == null || response.getCode() == null) {
            log.error("Get unread count failed: response is null, userId: {}", userId);
            return Result.error("服务调用异常，请稍后重试");
        }
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success(response.getUnreadCount());
        } else {
            log.warn("Get unread count failed: {}, userId: {}", response.getMessage(), userId);
            return Result.error(response.getMessage());
        }
    }

    /**
     * 异步发送邮件
     */
    @PostMapping("/send_email")
    public Result<Boolean> sendEmail(@RequestParam("message") String content, @RequestParam("receiverId") Long to, @RequestParam("title") String title) {
        if(emailClient.sendEmail(UserInfoManager.getUserId(), to, UserInfoManager.getUsername(), title, content)){
            return Result.success();
        }
        return Result.error();
    }
}

package com.zixin.messageapi.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 站内信实体类
 * 
 * 功能说明:
 * 1. 支持个人消息和群发消息
 * 2. 消息状态管理(未读/已读/已撤回/已删除)
 * 3. 支持消息撤回功能
 * 4. 软删除机制
 */
@Data
@TableName("message_inbox")
public class Message {
    
    /**
     * 消息ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long messageId;
    
    /**
     * 消息类型 - 对应MessageType枚举
     * 1 - SYSTEM (系统通知)
     * 2 - PERSONAL (个人消息)
     * 3 - BROADCAST (群发消息)
     * 4 - ANNOUNCEMENT (公告)
     */
    private Integer messageType;
    
    /**
     * 发送者ID
     * 系统消息时为null或0
     */
    private Long senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 接收者ID
     * 统一使用此字段，群发消息时为每个接收者创建一条记录
     */
    private Long receiverId;
    
    /**
     * 接收者名称
     */
    private String receiverName;
    
    /**
     * 群发消息组ID
     * 群发消息时使用，用于标识同一群发消息组的所有消息
     * 个人消息时此字段为null
     */
    private Long groupMessageId;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息状态 - 对应MessageStatus枚举
     * 0 - UNREAD (未读)
     * 1 - READ (已读)
     * 2 - REVOKED (已撤回)
     * 3 - DELETED (已删除)
     */
    private Integer status;
    
    /**
     * 是否群发消息
     * 0 - 否
     * 1 - 是
     */
    private Integer isBroadcast;
    
    /**
     * 阅读时间(Unix毫秒时间戳)
     */
    private Long readTime;
    
    /**
     * 撤回时间(Unix毫秒时间戳)
     */
    private Long revokeTime;
    
    /**
     * 创建时间(Unix毫秒时间戳)
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;
    
    /**
     * 更新时间(Unix毫秒时间戳)
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;
    
    /**
     * 删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}

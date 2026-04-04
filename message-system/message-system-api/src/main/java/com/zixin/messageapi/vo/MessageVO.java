package com.zixin.messageapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 消息详情VO
 */
@Data
public class MessageVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 消息类型描述
     */
    private String messageTypeDesc;
    
    /**
     * 发送者ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 接收者ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;
    
    /**
     * 接收者名称
     */
    private String receiverName;
    
    /**
     * 所有收件人名称列表(群发消息时使用)
     */
    private List<String> recipientNames;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息状态
     */
    private Integer status;
    
    /**
     * 消息状态描述
     */
    private String statusDesc;
    
    /**
     * 是否群发消息
     */
    private Integer isBroadcast;
    
    /**
     * 阅读时间
     */
    private Long readTime;
    
    /**
     * 撤回时间
     */
    private Long revokeTime;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
}

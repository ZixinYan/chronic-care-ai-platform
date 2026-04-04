package com.zixin.messageapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息查询请求DTO
 */
@Data
public class QueryMessageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 是否只查询未读消息
     */
    private Boolean unreadOnly;

    /**
     * 根据用户进行过滤
     */
    private Long userId;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}

package com.zixin.accountapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetUsersListRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    private String keyword;
    
    private Integer roleCode;
    
    private Integer status;
}

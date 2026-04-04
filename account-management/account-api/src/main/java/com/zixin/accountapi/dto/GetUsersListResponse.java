package com.zixin.accountapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetUsersListResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<UserItemDTO> users;
    
    private Long total;
    
    private Integer pageNum;
    
    private Integer pageSize;
    
    @Data
    public static class UserItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long userId;
        
        private String username;
        
        private String nickname;
        
        private Integer gender;
        
        private String phone;
        
        private String email;
        
        private String avatarUrl;
        
        private String address;
        
        private Long birthday;
        
        private Long createTime;
        
        private Long updateTime;
        
        private List<String> roles;
        
        private List<Integer> roleCodes;
    }
}

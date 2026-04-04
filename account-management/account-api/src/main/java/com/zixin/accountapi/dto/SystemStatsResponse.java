package com.zixin.accountapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemStatsResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long totalUsers;
    
    private Long totalDoctors;
    
    private Long totalPatients;
    
    private Long totalAdmins;
    
    private Long todayNewUsers;
    
    private Long activeUsersToday;
}

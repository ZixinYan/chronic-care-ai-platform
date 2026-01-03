package com.zixin.accountapi.dto;

import lombok.Data;

@Data
public class GetUserInfoRequest {
    private Long[] userIds;
}

package com.zixin.accountapi.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetUserInfoRequest {
    private List<Long> userIds;
}

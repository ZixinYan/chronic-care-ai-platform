package com.zixin.accountapi.dto;

import lombok.Data;

@Data
public class DeleteUserRequest {
    private Long[] userId;
}

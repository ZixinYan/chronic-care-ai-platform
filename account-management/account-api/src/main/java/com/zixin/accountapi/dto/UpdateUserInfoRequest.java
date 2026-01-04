package com.zixin.accountapi.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Data
public class UpdateUserInfoRequest {
    private Map<String, Objects> updateData;
}

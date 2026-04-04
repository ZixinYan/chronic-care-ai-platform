package com.zixin.accountapi.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Data
public class UpdateUserInfoRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Object> updateData;
}

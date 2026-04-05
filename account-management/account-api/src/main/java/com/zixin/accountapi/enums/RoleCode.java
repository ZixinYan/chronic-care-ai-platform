package com.zixin.accountapi.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum RoleCode {

    DOCTOR(1, "医生"),
    PATIENT(2, "患者"),
    ADMIN(4, "管理员");

    private static final Map<Integer, RoleCode> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(RoleCode::getCode, r -> r));

    private final int code;
    private final String desc;

    RoleCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RoleCode fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("RoleCode cannot be null");
        }
        RoleCode role = CODE_MAP.get(code);
        if (role == null) {
            throw new IllegalArgumentException("Invalid RoleCode: " + code);
        }
        return role;
    }
}

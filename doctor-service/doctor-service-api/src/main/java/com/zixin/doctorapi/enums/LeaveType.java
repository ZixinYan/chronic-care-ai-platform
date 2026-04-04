package com.zixin.doctorapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 医生请假类型枚举
 */
@Getter
@AllArgsConstructor
public enum LeaveType {

    SICK("SICK", "病假"),
    ANNUAL("ANNUAL", "年假"),
    PERSONAL("PERSONAL", "事假"),
    TRAINING("TRAINING", "培训"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    public static LeaveType fromCode(String code) {
        for (LeaveType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown leave type: " + code);
    }
}


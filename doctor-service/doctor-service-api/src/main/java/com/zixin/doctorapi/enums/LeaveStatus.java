package com.zixin.doctorapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 医生请假单状态枚举
 */
@Getter
@AllArgsConstructor
public enum LeaveStatus {

    /**
     * 待审批
     */
    PENDING("PENDING", "待审批"),

    /**
     * 已通过
     */
    APPROVED("APPROVED", "已通过"),

    /**
     * 已拒绝
     */
    REJECTED("REJECTED", "已拒绝"),

    /**
     * 已撤销
     */
    CANCELLED("CANCELLED", "已撤销");

    private final String code;
    private final String description;

    public static LeaveStatus fromCode(String code) {
        for (LeaveStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown leave status: " + code);
    }
}


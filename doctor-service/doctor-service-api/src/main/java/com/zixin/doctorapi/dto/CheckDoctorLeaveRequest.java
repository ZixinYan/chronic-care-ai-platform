package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 检查医生休假请求
 */
@Data
public class CheckDoctorLeaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 医生账户ID
     */
    private Long doctorId;

    /**
     * 检查日期（YYYY-MM-DD）
     */
    private String checkDay;
}

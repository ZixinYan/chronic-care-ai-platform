package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新请假单请求
 */
@Data
public class UpdateLeaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 请假单ID
     */
    private Long leaveId;

    /**
     * 医生账户ID（用于权限校验）
     */
    private Long doctorId;

    /**
     * 请假类型（可选）
     */
    private String leaveType;

    /**
     * 请假开始日期 (YYYY-MM-DD，可选)
     */
    private String startDay;

    /**
     * 请假结束日期 (YYYY-MM-DD，可选)
     */
    private String endDay;

    /**
     * 请假开始时间（毫秒时间戳，可选）
     */
    private Long startTime;

    /**
     * 请假结束时间（毫秒时间戳，可选）
     */
    private Long endTime;

    /**
     * 请假原因（可选）
     */
    private String reason;

    /**
     * 请假状态（可选，审批/撤销场景）
     */
    private String status;
}


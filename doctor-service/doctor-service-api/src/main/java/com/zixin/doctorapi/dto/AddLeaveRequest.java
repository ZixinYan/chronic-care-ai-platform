package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增请假单请求
 */
@Data
public class AddLeaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 医生账户ID
     */
    private Long doctorId;

    /**
     * 医生姓名（可选，不传则后端通过账户服务补全）
     */
    private String doctorName;

    /**
     * 请假类型（LeaveType.code）
     */
    private String leaveType;

    /**
     * 请假开始日期 (YYYY-MM-DD)
     */
    private String startDay;

    /**
     * 请假结束日期 (YYYY-MM-DD)
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
     * 请假原因
     */
    private String reason;
}


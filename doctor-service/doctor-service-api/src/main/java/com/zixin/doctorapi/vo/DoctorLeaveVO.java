package com.zixin.doctorapi.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 医生请假 VO
 */
@Data
public class DoctorLeaveVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long doctorId;
    private String doctorName;

    private String leaveType;
    private String leaveTypeDesc;

    private String status;
    private String statusDesc;

    private String startDay;
    private String endDay;
    private Long startTime;
    private Long endTime;

    private String reason;
}


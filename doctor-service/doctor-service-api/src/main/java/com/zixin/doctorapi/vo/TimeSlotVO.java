package com.zixin.doctorapi.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class TimeSlotVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long startTime;

    private Long endTime;

    private String startTimeStr;

    private String endTimeStr;

    private Boolean available;
}

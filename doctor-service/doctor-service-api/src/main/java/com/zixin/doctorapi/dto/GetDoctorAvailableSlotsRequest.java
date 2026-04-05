package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetDoctorAvailableSlotsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long doctorId;

    private String scheduleDay;

    private Integer slotDurationMinutes = 30;
}

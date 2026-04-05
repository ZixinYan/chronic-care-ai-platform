package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RecommendedDoctorVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long doctorId;

    private String doctorName;

    private String avatar;

    private String department;

    private String title;

    private String hospitalName;

    private String recommendation;
}

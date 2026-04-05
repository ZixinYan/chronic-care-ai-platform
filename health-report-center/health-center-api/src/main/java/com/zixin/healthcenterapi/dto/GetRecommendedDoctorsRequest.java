package com.zixin.healthcenterapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetRecommendedDoctorsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long reportId;
}

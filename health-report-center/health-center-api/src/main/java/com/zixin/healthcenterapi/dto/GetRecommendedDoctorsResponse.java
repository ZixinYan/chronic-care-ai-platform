package com.zixin.healthcenterapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetRecommendedDoctorsResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<RecommendedDoctorVO> doctors;

    private String aiRecommendation;
}

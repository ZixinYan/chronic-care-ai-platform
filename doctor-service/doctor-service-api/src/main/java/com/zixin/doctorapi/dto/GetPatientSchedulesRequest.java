package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetPatientSchedulesRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long patientId;
    
    private String status;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
}

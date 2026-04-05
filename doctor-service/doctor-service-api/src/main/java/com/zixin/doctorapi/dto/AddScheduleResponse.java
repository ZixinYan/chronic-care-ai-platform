package com.zixin.doctorapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;

@Data
public class AddScheduleResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long scheduleId;
}

package com.zixin.doctorapi.dto;

import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.utils.utils.BaseResponse;
import com.zixin.utils.utils.PageUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetPatientSchedulesResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private PageUtils schedules;
}

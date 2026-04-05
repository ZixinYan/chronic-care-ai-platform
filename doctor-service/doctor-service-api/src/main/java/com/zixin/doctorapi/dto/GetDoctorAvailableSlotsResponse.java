package com.zixin.doctorapi.dto;

import com.zixin.doctorapi.vo.TimeSlotVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetDoctorAvailableSlotsResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<TimeSlotVO> availableSlots;
}

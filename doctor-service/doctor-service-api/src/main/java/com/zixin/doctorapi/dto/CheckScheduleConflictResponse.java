package com.zixin.doctorapi.dto;

import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CheckScheduleConflictResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean hasConflict;

    private List<ScheduleVO> conflictingSchedules;
}

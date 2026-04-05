package com.zixin.doctorapi.dto;

import com.zixin.doctorapi.vo.DoctorLeaveVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;

/**
 * 检查医生休假响应
 */
@Data
public class CheckDoctorLeaveResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 是否在休假中
     */
    private Boolean onLeave;

    /**
     * 休假信息（如果在休假中，返回休假详情）
     */
    private DoctorLeaveVO leaveInfo;
}

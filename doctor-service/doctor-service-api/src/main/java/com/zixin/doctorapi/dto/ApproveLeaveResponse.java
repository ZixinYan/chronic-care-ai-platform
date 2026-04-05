package com.zixin.doctorapi.dto;

import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

import java.io.Serializable;

/**
 * 审批休假响应
 */
@Data
public class ApproveLeaveResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private Boolean success;
}

package com.zixin.doctorapi.dto;

import com.zixin.utils.utils.BaseResponse;

import java.io.Serializable;

/**
 * 新增请假单响应
 */
public class AddLeaveResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 新建的请假单ID
     */
    private Long leaveId;

    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }
}


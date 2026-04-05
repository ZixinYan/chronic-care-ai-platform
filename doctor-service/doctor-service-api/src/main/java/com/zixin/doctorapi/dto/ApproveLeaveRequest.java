package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批休假请求
 */
@Data
public class ApproveLeaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 请假单ID
     */
    private Long leaveId;

    /**
     * 审批状态（APPROVED/REJECTED）
     */
    private String status;

    /**
     * 审批意见（可选）
     */
    private String approvalComment;

    /**
     * 审批人ID（管理员ID）
     */
    private Long approverId;
}

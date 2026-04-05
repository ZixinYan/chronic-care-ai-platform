package com.zixin.doctorapi.api;

import com.zixin.doctorapi.dto.*;

/**
 * 医生请假 Dubbo 服务接口
 *
 * 提供请假单的增删改查能力
 */
public interface DoctorLeaveAPI {

    /**
     * 新增请假单
     */
    AddLeaveResponse addLeave(AddLeaveRequest request);

    /**
     * 更新请假单（时间、类型、原因或状态）
     */
    UpdateLeaveResponse updateLeave(UpdateLeaveRequest request);

    /**
     * 删除请假单（逻辑删除）
     */
    DeleteLeaveResponse deleteLeave(Long leaveId, Long doctorId);

    /**
     * 分页查询请假单
     */
    QueryLeaveResponse queryLeaves(QueryLeaveRequest request);

    /**
     * 检查医生在指定日期是否休假
     */
    CheckDoctorLeaveResponse checkDoctorLeave(CheckDoctorLeaveRequest request);

    /**
     * 管理员审批休假申请
     */
    ApproveLeaveResponse approveLeave(ApproveLeaveRequest request);

    /**
     * 管理员查询待审批休假列表
     */
    QueryLeaveResponse queryPendingLeaves(QueryLeaveRequest request);
}


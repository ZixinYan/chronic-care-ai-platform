package com.zixin.doctorconsumer.controller;

import com.zixin.doctorapi.api.DoctorLeaveAPI;
import com.zixin.doctorapi.dto.*;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.security.RequireRole;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctor/leave")
@Slf4j
public class DoctorLeaveController {

    @DubboReference(check = false)
    private DoctorLeaveAPI leaveAPI;

    @PostMapping("/add")
    @RequireRole("DOCTOR")
    public Result<Long> addLeave(@RequestBody AddLeaveRequest request) {
        request.setDoctorId(UserInfoManager.getUserId());
        log.info("Add leave request, doctorId: {}, leaveType: {}", request.getDoctorId(), request.getLeaveType());

        AddLeaveResponse response = leaveAPI.addLeave(request);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response.getLeaveId());
        } else {
            return Result.error(response.getMessage());
        }
    }

    @PostMapping("/update")
    @RequireRole("DOCTOR")
    public Result<Boolean> updateLeave(@RequestBody UpdateLeaveRequest request) {
        request.setDoctorId(UserInfoManager.getUserId());
        log.info("Update leave request, leaveId: {}, doctorId: {}", request.getLeaveId(), request.getDoctorId());

        UpdateLeaveResponse response = leaveAPI.updateLeave(request);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(true);
        } else {
            return Result.error(response.getMessage());
        }
    }

    @DeleteMapping("/delete/{leaveId}")
    @RequireRole("DOCTOR")
    public Result<Boolean> deleteLeave(@PathVariable("leaveId") Long leaveId) {
        Long doctorId = UserInfoManager.getUserId();
        log.info("Delete leave request, leaveId: {}, doctorId: {}", leaveId, doctorId);

        DeleteLeaveResponse response = leaveAPI.deleteLeave(leaveId, doctorId);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(true);
        } else {
            return Result.error(response.getMessage());
        }
    }

    @PostMapping("/list")
    @RequireRole("DOCTOR")
    public Result<QueryLeaveResponse> queryLeaves(@RequestBody QueryLeaveRequest request) {
        request.setDoctorId(UserInfoManager.getUserId());
        log.info("Query leaves request, doctorId: {}, status: {}", request.getDoctorId(), request.getStatus());

        QueryLeaveResponse response = leaveAPI.queryLeaves(request);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }
}

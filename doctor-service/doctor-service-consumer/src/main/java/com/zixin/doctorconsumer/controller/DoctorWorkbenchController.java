package com.zixin.doctorconsumer.controller;

import com.zixin.doctorapi.api.DoctorWorkbenchAPI;
import com.zixin.doctorapi.dto.*;
import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.security.RequireRole;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * 医生工作台Controller
 * 功能:
 * 1. AI生成日程建议
 * 2. 查看日程表
 * 3. 完成日程并上传诊断报告
 * 4. 取消日程
 * 5. 更新日程状态
 * @author zixin
 */
@RestController
@RequestMapping("/doctor/workbench")
@Slf4j
public class DoctorWorkbenchController {

    @DubboReference(check = false)
    private DoctorWorkbenchAPI workbenchAPI;

    /**
     * 添加日程
     * @param request
     * @return
     */
    @PostMapping("/schedule/add")
    @RequireRole("DOCTOR")
    public Result<Boolean> addSchedule(@RequestBody AddScheduleRequest request) {
        log.info("Add schedule request, doctorId: {}, date: {}", request.getSchedule().getScheduleDay());
        AddScheduleResponse response = workbenchAPI.addSchedule(request);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(true);
        } else {
            return Result.error(response.getMessage());
        }
    }

    /**
     * 查看日程表
     *
     * 权限要求:
     * - 角色: DOCTOR
     * @param request 查询条件
     * @return 日程列表
     */
    @GetMapping("/schedule/list")
    @RequireRole("DOCTOR")
    public Result<QueryScheduleResponse> querySchedule(@RequestBody QueryScheduleRequest request) {

        // 强制使用JWT中的userId,防止越权查询
        request.setDoctorId(UserInfoManager.getUserId());

        log.info("Query schedule request, doctorId: {}, date: {}",
                UserInfoManager.getUserId(), request.getScheduleDay());

        QueryScheduleResponse response = workbenchAPI.querySchedule(request);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }

    /**
     * 获取日程详情
     *
     * 权限要求:
     * - 角色: DOCTOR
     * - 权限: doctor:read (医生读权限)
     *
     * 安全策略:
     * - 从X-User-Id Header获取医生ID
     * - Provider层会验证日程归属,防止查看其他医生的日程
     *
     * @param scheduleId 日程ID
     * @return 日程详情
     */
    @GetMapping("/schedule/detail")
    @RequireRole("DOCTOR")
    public Result<ScheduleVO> getScheduleDetail(
            @RequestParam("scheduleId") Long scheduleId) {

        log.info("Get schedule detail, scheduleId: {}, doctorId: {}", scheduleId, UserInfoManager.getUserId());

        // Provider层会验证schedule.doctorId == userId
        GetScheduleDetailResponse response = workbenchAPI.getScheduleDetail(scheduleId, UserInfoManager.getUserId());

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response.getSchedule());
        } else {
            return Result.error(response.getMessage());
        }
    }

    /**
     * 完成日程并上传诊断报告
     * @param request 完成日程请求
     * @return 完成结果
     */
    @PostMapping("/schedule/complete")
    @RequireRole("DOCTOR")
    public Result<CompleteScheduleResponse> completeSchedule(
            @RequestBody CompleteScheduleRequest request) {

        // 使用JWT中的userId
        request.setDoctorId(UserInfoManager.getUserId());
        log.info("Complete schedule request, scheduleId: {}, doctorId: {}",
                request.getScheduleId(), UserInfoManager.getUserId());

        CompleteScheduleResponse response = workbenchAPI.completeSchedule(request);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }

    /**
     * 取消日程
     * @param scheduleId 日程ID
     * @param reason 取消原因
     * @return 取消结果
     */
    @PostMapping("/schedule/cancel")
    @RequireRole("DOCTOR")
    public Result<Boolean> cancelSchedule(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("reason") String reason) {

        log.info("Cancel schedule request, scheduleId: {}, doctorId: {}, reason: {}",
                scheduleId, UserInfoManager.getUserId(), reason);

        // Provider层会验证日程归属
        CancelScheduleResponse response = workbenchAPI.cancelSchedule(scheduleId, UserInfoManager.getUserId(), reason);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(true);
        } else {
            return Result.error(response.getMessage());
        }
    }

    /**
     * 更新日程状态
     * @param scheduleId 日程ID
     * @param status 新状态 (PENDING/IN_PROGRESS/COMPLETED/CANCELLED)
     * @return 更新结果
     */
    @PostMapping("/schedule/status")
    @RequireRole("DOCTOR")
    public Result<Boolean> updateScheduleStatus(
            @RequestParam(value = "scheduleId") Long scheduleId,
            @RequestParam(value = "status") String status ) {

        log.info("Update schedule status, scheduleId: {}, doctorId: {}, status: {}",
                scheduleId, UserInfoManager.getUserId(), status);

        // Provider层会验证日程归属和状态流转
        UpdateScheduleStatusResponse response = workbenchAPI.updateScheduleStatus(scheduleId, UserInfoManager.getUserId(), status);

        if (response.getCode() == ToBCodeEnum.SUCCESS) {
            return Result.success(true);
        } else {
            return Result.error(response.getMessage());
        }
    }
}

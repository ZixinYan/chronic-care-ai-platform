package com.zixin.healthcenterprovider.client;

import com.zixin.doctorapi.api.DoctorLeaveAPI;
import com.zixin.doctorapi.api.DoctorWorkbenchAPI;
import com.zixin.doctorapi.dto.*;
import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.BusinessException;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class DoctorClient {

    @DubboReference(timeout = 50000)
    private DoctorWorkbenchAPI doctorWorkbenchAPI;

    @DubboReference(timeout = 50000)
    private DoctorLeaveAPI doctorLeaveAPI;

    private final ExecutorService scheduleExecutor = Executors.newFixedThreadPool(2);



    public CompletableFuture<Boolean> addScheduleAsync(Long doctorId, Long patientId, String doctorName, ScheduleVO scheduleVO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("开始异步添加排班, doctorId: {}, doctorName: {}, scheduleDate: {}",
                        doctorId, doctorName, scheduleVO.getScheduleDay());

                AddScheduleRequest request = new AddScheduleRequest();
                request.setDoctorId(doctorId);
                request.setDoctorName(doctorName);
                request.setSchedule(scheduleVO);
                request.setPatientId(patientId);
                AddScheduleResponse response = doctorWorkbenchAPI.addSchedule(request);

                if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
                    log.info("异步添加排班成功, doctorId: {}, scheduleId: {}",
                            doctorId, response.getScheduleId());
                    return true;
                } else {
                    log.error("异步添加排班失败, doctorId: {}, error: {}",
                            doctorId, response.getMessage());
                    return false;
                }
            } catch (Exception e) {
                log.error("异步添加排班异常, doctorId: {}", doctorId, e);
                throw new RuntimeException("排班添加异常", e);
            }
        }, scheduleExecutor);
    }

    /**
     * 同步添加排班
     * @param doctorId 医生ID
     * @param patientId 患者ID
     * @param doctorName 医生姓名
     * @param scheduleVO 排班信息
     * @return 日程ID
     * @throws BusinessException 添加失败时抛出，包含具体失败原因（如医生休假、日程冲突等）
     */
    public Long addSchedule(Long doctorId, Long patientId, String doctorName, ScheduleVO scheduleVO) {
        try {
            log.debug("开始添加排班, doctorId: {}, doctorName: {}, scheduleDate: {}",
                    doctorId, doctorName, scheduleVO.getScheduleDay());

            AddScheduleRequest request = new AddScheduleRequest();
            request.setDoctorId(doctorId);
            request.setDoctorName(doctorName);
            request.setSchedule(scheduleVO);
            request.setPatientId(patientId);

            AddScheduleResponse response = doctorWorkbenchAPI.addSchedule(request);

            if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
                log.info("添加排班成功, doctorId: {}, scheduleId: {}",
                        doctorId, response.getScheduleId());
                return response.getScheduleId();
            } else {
                String errorMsg = response.getMessage() != null ? response.getMessage() : "添加排班失败";
                log.error("添加排班失败, doctorId: {}, error: {}", doctorId, errorMsg);
                throw new BusinessException(errorMsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加排班异常, doctorId: {}", doctorId, e);
            throw new BusinessException("排班添加异常: " + e.getMessage(), e);
        }
    }

    /**
     * 检查医生在指定日期是否有空（预约逻辑参考）
     *
     * 检查项:
     * 1. 医生是否在当天休假
     * 2. 医生当天是否有日程冲突
     *
     * @param doctorId 医生ID
     * @param checkDay 检查日期 (YYYY-MM-DD)
     * @param startTime 时间段开始时间（毫秒时间戳）
     * @param endTime 时间段结束时间（毫秒时间戳）
     * @param excludeScheduleId 排除的日程ID（可选，用于切换医生场景）
     * @return 可用性检查结果，为空字符串表示可用，否则返回具体不可用原因
     */
    public String checkDoctorAvailability(Long doctorId, String checkDay, Long startTime, Long endTime, Long excludeScheduleId) {
        // 1. 检查医生是否休假
        try {
            CheckDoctorLeaveRequest leaveRequest = new CheckDoctorLeaveRequest();
            leaveRequest.setDoctorId(doctorId);
            leaveRequest.setCheckDay(checkDay);
            CheckDoctorLeaveResponse leaveResponse = doctorLeaveAPI.checkDoctorLeave(leaveRequest);

            if (leaveResponse.getOnLeave() != null && leaveResponse.getOnLeave()) {
                log.warn("checkDoctorAvailability - 医生休假, doctorId: {}, day: {}", doctorId, checkDay);
                return "医生在当天休假，无法发送报告审批。";
            }
        } catch (Exception e) {
            log.error("checkDoctorAvailability - 检查医生休假状态异常, doctorId: {}, day: {}", doctorId, checkDay, e);
            return "检查医生休假状态异常: " + e.getMessage();
        }

        // 2. 检查日程冲突
        try {
            CheckScheduleConflictRequest conflictRequest = new CheckScheduleConflictRequest();
            conflictRequest.setDoctorId(doctorId);
            conflictRequest.setScheduleDay(checkDay);
            conflictRequest.setStartTime(startTime);
            conflictRequest.setEndTime(endTime);
            conflictRequest.setExcludeScheduleId(excludeScheduleId);

            CheckScheduleConflictResponse conflictResponse = doctorWorkbenchAPI.checkScheduleConflict(conflictRequest);

            if (ToBCodeEnum.SUCCESS.equals(conflictResponse.getCode())
                    && conflictResponse.getHasConflict() != null
                    && conflictResponse.getHasConflict()) {
                log.warn("checkDoctorAvailability - 日程冲突, doctorId: {}, day: {}", doctorId, checkDay);
                return "医生在该时间段已有日程安排，请选择其他时间或更换医生。";
            }
        } catch (Exception e) {
            log.error("checkDoctorAvailability - 检查日程冲突异常, doctorId: {}, day: {}", doctorId, checkDay, e);
            return "检查医生日程状态异常: " + e.getMessage();
        }

        // 可用
        return "";
    }

    /**
     * 获取当天默认时间范围（0点到次日0点）
     */
    public static long[] getTodayDefaultTimeRange() {
        LocalDate today = LocalDate.now();
        long startTime = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new long[]{startTime, endTime};
    }

    /**
     * 获取今天的日期字符串
     */
    public static String getTodayStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 取消日程
     * @param scheduleId 日程ID
     * @param doctorId 医生ID
     * @param reason 取消原因
     * @return true-成功 false-失败
     */
    public boolean cancelSchedule(Long scheduleId, Long doctorId, String reason) {
        try {
            log.debug("开始取消日程, scheduleId: {}, doctorId: {}, reason: {}",
                    scheduleId, doctorId, reason);

            CancelScheduleResponse response = doctorWorkbenchAPI.cancelSchedule(scheduleId, doctorId, reason);

            if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
                log.info("取消日程成功, scheduleId: {}, doctorId: {}", scheduleId, doctorId);
                return true;
            } else {
                log.error("取消日程失败, scheduleId: {}, doctorId: {}, error: {}",
                        scheduleId, doctorId, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("取消日程异常, scheduleId: {}, doctorId: {}", scheduleId, doctorId, e);
            return false;
        }
    }

}

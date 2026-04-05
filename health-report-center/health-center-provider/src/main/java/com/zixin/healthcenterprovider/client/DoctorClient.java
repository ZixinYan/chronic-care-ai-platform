package com.zixin.healthcenterprovider.client;

import com.zixin.doctorapi.api.DoctorWorkbenchAPI;
import com.zixin.doctorapi.dto.AddScheduleRequest;
import com.zixin.doctorapi.dto.AddScheduleResponse;
import com.zixin.doctorapi.dto.CancelScheduleResponse;
import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class DoctorClient {

    @DubboReference(timeout = 50000)
    private DoctorWorkbenchAPI doctorWorkbenchAPI;

    private final ExecutorService scheduleExecutor = Executors.newFixedThreadPool(10);



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
     * @return 日程ID，失败返回null
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
                log.error("添加排班失败, doctorId: {}, error: {}",
                        doctorId, response.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("添加排班异常, doctorId: {}", doctorId, e);
            throw new RuntimeException("排班添加异常", e);
        }
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

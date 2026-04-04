package com.zixin.doctorapi.api;

import com.zixin.doctorapi.dto.*;

/**
 * 医生工作台Dubbo服务接口
 * 
 * 提供医生日程管理、AI日程推荐等功能
 */
public interface DoctorWorkbenchAPI {
    /**
     * 查询医生日程
     * 
     * 支持按日期、状态、类别等条件查询
     *
     * @param request 查询条件
     * @return 日程列表
     */
    QueryScheduleResponse querySchedule(QueryScheduleRequest request);
    
    /**
     * 获取日程详情
     *
     * @param scheduleId 日程ID
     * @param doctorId 医生ID
     * @return 日程详情
     */
    GetScheduleDetailResponse getScheduleDetail(Long scheduleId, Long doctorId);
    
    /**
     * 完成日程并上传诊断报告
     *
     * @param request 完成日程请求
     * @return 完成结果
     */
    CompleteScheduleResponse completeSchedule(CompleteScheduleRequest request);
    
    /**
     * 取消日程
     *
     * @param scheduleId 日程ID
     * @param doctorId 医生ID
     * @param reason 取消原因
     * @return 取消结果
     */
    CancelScheduleResponse cancelSchedule(Long scheduleId, Long doctorId, String reason);
    
    /**
     * 更新日程状态
     *
     * @param scheduleId 日程ID
     * @param doctorId 医生ID
     * @param status 新状态
     * @return 更新结果
     */
    UpdateScheduleStatusResponse updateScheduleStatus(Long scheduleId, Long doctorId, String status);

    /**
     * 添加新日程
     * @param request
     * @return
     */
    AddScheduleResponse addSchedule(AddScheduleRequest request);


}

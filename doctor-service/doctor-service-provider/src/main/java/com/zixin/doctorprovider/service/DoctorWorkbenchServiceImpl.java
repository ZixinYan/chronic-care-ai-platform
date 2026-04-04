package com.zixin.doctorprovider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zixin.accountapi.dto.GetDoctorInfoRequest;
import com.zixin.accountapi.dto.GetPatientInfoRequest;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordRequest;
import com.zixin.aicapabilityapi.vo.MedicalRecordVO;
import com.zixin.doctorapi.api.DoctorWorkbenchAPI;
import com.zixin.doctorapi.dto.*;
import com.zixin.doctorapi.enums.ScheduleCategory;
import com.zixin.doctorapi.enums.SchedulePriority;
import com.zixin.doctorapi.enums.ScheduleStatus;
import com.zixin.doctorapi.po.DoctorSchedule;
import com.zixin.doctorapi.po.MedicalRecord;
import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.doctorprovider.client.AiClient;
import com.zixin.doctorprovider.client.DoctorClient;
import com.zixin.doctorprovider.mapper.DoctorScheduleMapper;
import com.zixin.doctorprovider.mapper.MedicalRecordMapper;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 医生工作台服务实现 (Dubbo服务)
 * 
 * 提供医生日程管理、AI推荐等功能
 * 
 * 权限验证策略:
 * - Consumer层已验证DOCTOR角色和相关权限
 * - Provider层进行业务级权限验证:
 *   1. 数据归属验证(医生只能操作自己的日程)
 *   2. 状态流转验证(日程状态合法性检查)
 *   3. 业务规则验证(如:诊断报告不能为空)
 * 
 * 安全原则:
 * - 所有操作必须验证doctorId与schedule.doctorId一致
 * - 记录所有权限验证失败的日志,便于审计
 * - 使用@Transactional保证数据一致性
 * 
 * @author zixin
 */
@Service
@DubboService
@Slf4j
public class DoctorWorkbenchServiceImpl implements DoctorWorkbenchAPI {
    private final DoctorScheduleMapper scheduleMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final DoctorClient doctorClient;
    private final AiClient aiClient;

    public DoctorWorkbenchServiceImpl(DoctorScheduleMapper scheduleMapper,
                                       MedicalRecordMapper medicalRecordMapper,
                                       DoctorClient doctorClient,
                                       AiClient aiClient) {
        this.scheduleMapper = scheduleMapper;
        this.medicalRecordMapper = medicalRecordMapper;
        this.doctorClient = doctorClient;
        this.aiClient = aiClient;
    }

    @Override
    public QueryScheduleResponse querySchedule(QueryScheduleRequest request) {
        QueryScheduleResponse response = new QueryScheduleResponse();

        try {
            // 1. 创建 MyBatis-Plus 的分页对象
            Page<DoctorSchedule> page = new Page<>(
                    request.getPageNum(),
                    request.getPageSize()
            );

            // 2. 构建查询条件（doctorId、scheduleDay 至少填一项；仅 scheduleDay 时用于 AI 跨医生按日聚合）
            if (request.getDoctorId() == null
                    && (request.getScheduleDay() == null || request.getScheduleDay().isEmpty())) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("doctorId 与 scheduleDay 不能同时为空");
                return response;
            }

            LambdaQueryWrapper<DoctorSchedule> wrapper = new LambdaQueryWrapper<>();

            if (request.getDoctorId() != null) {
                wrapper.eq(DoctorSchedule::getDoctorId, request.getDoctorId());
            }
            if (request.getScheduleDay() != null && !request.getScheduleDay().isEmpty()) {
                wrapper.eq(DoctorSchedule::getScheduleDay, request.getScheduleDay());
            }
            if (request.getStatus() != null) {
                wrapper.eq(DoctorSchedule::getStatus, request.getStatus());
            }
            if (request.getScheduleCategoryId() != null) {
                wrapper.eq(DoctorSchedule::getScheduleCategory, request.getScheduleCategoryId());
            }

            // 排序
            wrapper.orderByDesc(DoctorSchedule::getPriority)
                    .orderByDesc(DoctorSchedule::getCreateTime);

            // 3. 执行分页查询
            Page<DoctorSchedule> schedulePage = scheduleMapper.selectPage(page, wrapper);

            // 4. 使用自定义 PageUtils 封装分页数据
            PageUtils pageUtils = new PageUtils(schedulePage);

            // 5. 转换VO（如果需要转换的话）
            if (pageUtils.getList() != null && !pageUtils.getList().isEmpty()) {
                List<ScheduleVO> scheduleVOS = ((List<DoctorSchedule>) pageUtils.getList()).stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());
                pageUtils.setList(scheduleVOS);
            }

            // 6. 构建返回对象
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setSchedules(pageUtils);

            log.info("Query schedule success, doctorId: {}, total: {}, pageNum: {}, pageSize: {}",
                    request.getDoctorId(), pageUtils.getTotalCount(),
                    request.getPageNum(), request.getPageSize());

        } catch (Exception e) {
            log.error("Query schedule error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询日程失败: " + e.getMessage());
        }

        return response;
    }
    
    @Override
    public GetScheduleDetailResponse getScheduleDetail(Long scheduleId, Long doctorId) {
        GetScheduleDetailResponse response = new GetScheduleDetailResponse();
        
        try {
            // 1. 查询日程
            DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
            
            if (schedule == null) {
                log.warn("Schedule not found, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("日程不存在");
                return response;
            }
            
            // 2. 只能查看自己的日程
            if (!schedule.getDoctorId().equals(doctorId)) {
                log.warn("Permission denied: doctor {} tried to access schedule {} owned by doctor {}", 
                        doctorId, scheduleId, schedule.getDoctorId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权访问该日程");
                return response;
            }
            
            // 3. 返回日程详情
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setSchedule(convertToVO(schedule));
            
            log.info("Get schedule detail success, scheduleId: {}, doctorId: {}", scheduleId, doctorId);
        } catch (Exception e) {
            log.error("Get schedule detail failed, scheduleId: {}, doctorId: {}", scheduleId, doctorId, e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询日程详情失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompleteScheduleResponse completeSchedule(CompleteScheduleRequest request) {
        CompleteScheduleResponse response = new CompleteScheduleResponse();
        
        try {
            // 1. 查询日程
            DoctorSchedule schedule = scheduleMapper.selectById(request.getScheduleId());
            
            if (schedule == null) {
                log.warn("Schedule not found, scheduleId: {}", request.getScheduleId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("日程不存在");
                return response;
            }
            
            // 2. 只能完成自己的日程
            if (!schedule.getDoctorId().equals(request.getDoctorId())) {
                log.warn("Permission denied: doctor {} tried to complete schedule {} owned by doctor {}", 
                        request.getDoctorId(), request.getScheduleId(), schedule.getDoctorId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权操作该日程");
                return response;
            }
            
            // 3. 只能完成待处理或进行中的日程
            if (ScheduleStatus.COMPLETED.getCode().equals(schedule.getStatus())) {
                log.warn("Schedule already completed, scheduleId: {}", request.getScheduleId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("日程已完成,无需重复操作");
                return response;
            }
            
            if (ScheduleStatus.CANCELLED.getCode().equals(schedule.getStatus())) {
                log.warn("Cannot complete cancelled schedule, scheduleId: {}", request.getScheduleId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("已取消的日程无法完成");
                return response;
            }
            
            // 4. 诊断报告不能为空
            if (request.getDiagnosisReport() == null || request.getDiagnosisReport().trim().isEmpty()) {
                log.warn("Diagnosis report is empty, scheduleId: {}", request.getScheduleId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("诊断报告不能为空");
                return response;
            }
            
            // 5. 更新日程状态和诊断报告
            long currentTime = System.currentTimeMillis();
            LambdaUpdateWrapper<DoctorSchedule> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(DoctorSchedule::getId, request.getScheduleId())
                   .eq(DoctorSchedule::getVersion, schedule.getVersion())  // 乐观锁
                   .set(DoctorSchedule::getStatus, ScheduleStatus.COMPLETED.getCode())
                   .set(DoctorSchedule::getResult, buildResult(request))
                   .set(DoctorSchedule::getUpdateTime, currentTime);
            
            int rows = scheduleMapper.update(null, wrapper);
            
            if (rows > 0) {
                // 查询更新后的日程
                DoctorSchedule updated = scheduleMapper.selectById(request.getScheduleId());

                // 异步生成电子病历
                generateMedicalRecordAsync(schedule, request);

                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("完成日程成功");
                response.setSchedule(convertToVO(updated));

                log.info("Complete schedule success, scheduleId: {}, doctorId: {}",
                        request.getScheduleId(), request.getDoctorId());
            } else {
                log.warn("Complete schedule failed due to version conflict, scheduleId: {}", request.getScheduleId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("完成日程失败,请刷新后重试");
            }
        } catch (Exception e) {
            log.error("Complete schedule failed, scheduleId: {}, doctorId: {}", 
                    request.getScheduleId(), request.getDoctorId(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("完成日程失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CancelScheduleResponse cancelSchedule(Long scheduleId, Long doctorId, String reason) {
        CancelScheduleResponse response = new CancelScheduleResponse();
        
        try {
            // 1. 查询日程
            DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
            
            if (schedule == null) {
                log.warn("Schedule not found, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("日程不存在");
                return response;
            }
            
            // 2. 只能取消自己的日程
            if (!schedule.getDoctorId().equals(doctorId)) {
                log.warn("Permission denied: doctor {} tried to cancel schedule {} owned by doctor {}", 
                        doctorId, scheduleId, schedule.getDoctorId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权操作该日程");
                return response;
            }
            
            // 3. 只能取消待处理或进行中的日程
            if (ScheduleStatus.COMPLETED.getCode().equals(schedule.getStatus())) {
                log.warn("Cannot cancel completed schedule, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("已完成的日程无法取消");
                return response;
            }
            
            if (ScheduleStatus.CANCELLED.getCode().equals(schedule.getStatus())) {
                log.warn("Schedule already cancelled, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("日程已取消,无需重复操作");
                return response;
            }
            
            // 4. 验证取消原因不能为空
            if (reason == null || reason.trim().isEmpty()) {
                log.warn("Cancel reason is empty, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("取消原因不能为空");
                return response;
            }
            
            // 5. 更新日程状态
            long currentTime = System.currentTimeMillis();
            LambdaUpdateWrapper<DoctorSchedule> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(DoctorSchedule::getId, scheduleId)
                   .eq(DoctorSchedule::getVersion, schedule.getVersion())
                   .set(DoctorSchedule::getStatus, ScheduleStatus.CANCELLED.getCode())
                   .set(DoctorSchedule::getResult, "取消原因: " + reason)
                   .set(DoctorSchedule::getUpdateTime, currentTime);
            
            int rows = scheduleMapper.update(null, wrapper);
            
            if (rows > 0) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("取消日程成功");
                response.setScheduleId(scheduleId);
                
                log.info("Cancel schedule success, scheduleId: {}, doctorId: {}, reason: {}", 
                        scheduleId, doctorId, reason);
            } else {
                log.warn("Cancel schedule failed due to version conflict, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("取消日程失败,请刷新后重试");
            }
        } catch (Exception e) {
            log.error("Cancel schedule failed, scheduleId: {}, doctorId: {}", scheduleId, doctorId, e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("取消日程失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UpdateScheduleStatusResponse updateScheduleStatus(Long scheduleId, Long doctorId, String status) {
        UpdateScheduleStatusResponse response = new UpdateScheduleStatusResponse();

        try {
            // 1. 验证状态合法性
            ScheduleStatus targetStatus;
            try {
                targetStatus = ScheduleStatus.fromCode(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid schedule status: {}", status);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无效的日程状态: " + status);
                return response;
            }
            if (targetStatus.equals(ScheduleStatus.COMPLETED) || targetStatus.equals(ScheduleStatus.CANCELLED)){
                log.error("Invalid operation: cannot set status to COMPLETED/CANCELLED using this endpoint, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("不允许通过这个接口完成: " + status);
                return response;
            }
            
            // 2. 查询日程
            DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
            
            if (schedule == null) {
                log.warn("Schedule not found, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("日程不存在");
                return response;
            }
            
            // 3. 只能更新自己的日程
            if (!schedule.getDoctorId().equals(doctorId)) {
                log.warn("Permission denied: doctor {} tried to update schedule {} owned by doctor {}", 
                        doctorId, scheduleId, schedule.getDoctorId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权操作该日程");
                return response;
            }
            
            // 4. 验证状态流转合法性
            ScheduleStatus currentStatus = ScheduleStatus.fromCode(schedule.getStatus());
            if (!isValidStatusTransition(currentStatus, targetStatus)) {
                log.warn("Invalid status transition from {} to {}, scheduleId: {}", 
                        currentStatus, targetStatus, scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage(String.format("无效的状态流转: %s -> %s", 
                        currentStatus.getDescription(), targetStatus.getDescription()));
                return response;
            }
            
            // 5. 更新日程状态
            long currentTime = System.currentTimeMillis();
            LambdaUpdateWrapper<DoctorSchedule> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(DoctorSchedule::getId, scheduleId)
                   .eq(DoctorSchedule::getVersion, schedule.getVersion())  // 乐观锁
                   .set(DoctorSchedule::getStatus, status)
                   .set(DoctorSchedule::getUpdateTime, currentTime);
            
            int rows = scheduleMapper.update(null, wrapper);
            
            if (rows > 0) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("更新状态成功");
                response.setScheduleId(scheduleId);
                response.setStatus(status);
                
                log.info("Update schedule status success, scheduleId: {}, doctorId: {}, {} -> {}", 
                        scheduleId, doctorId, currentStatus, targetStatus);
            } else {
                log.warn("Update schedule status failed due to version conflict, scheduleId: {}", scheduleId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("更新状态失败,请刷新后重试");
            }
        } catch (Exception e) {
            log.error("Update schedule status failed, scheduleId: {}, doctorId: {}, status: {}", 
                    scheduleId, doctorId, status, e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("更新状态失败: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    public AddScheduleResponse addSchedule(AddScheduleRequest request) {
        // 1. 构建 DoctorSchedule 对象
        DoctorSchedule schedule = new DoctorSchedule();
        BeanUtils.copyProperties(request.getSchedule(), schedule);
        if(request.getDoctorId() == null) {
            schedule.setDoctorId(UserInfoManager.getUserId());
            schedule.setDoctorName(UserInfoManager.getUsername());
        }else{
            schedule.setDoctorId(request.getDoctorId());
            schedule.setDoctorName(request.getDoctorName());
        }
        schedule.setStatus(ScheduleStatus.PENDING.getCode());  // 新增日程默认为待处理
        schedule.setCreateTime(System.currentTimeMillis());
        schedule.setUpdateTime(System.currentTimeMillis());
        schedule.setPatientId(request.getPatientId());
        PatientVO patientVO = doctorClient.getPatientInfo(GetPatientInfoRequest.builder()
                .userId(request.getPatientId())
                .build());
        schedule.setPatientName(patientVO != null ? patientVO.getUsername() : "unknown");

        // 2. 插入数据库
        int rows = scheduleMapper.insert(schedule);
        AddScheduleResponse response = new AddScheduleResponse();
        if (rows > 0) {
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("添加日程成功");
            log.info("Add schedule success, scheduleId: {}, doctorId: {}",
                    schedule.getId(), request.getDoctorId());
        } else {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("添加日程失败");
            log.warn("Add schedule failed, doctorId: {}", request.getDoctorId());
        }
        return response;
    }

    /**
     * 验证日程状态流转是否合法
     * 
     * 状态流转规则:
     * PENDING -> IN_PROGRESS, COMPLETED, CANCELLED
     * IN_PROGRESS -> COMPLETED, CANCELLED
     * COMPLETED -> (不允许流转)
     * CANCELLED -> (不允许流转)
     */
    private boolean isValidStatusTransition(ScheduleStatus from, ScheduleStatus to) {
        if (from == to) {
            return true;  // 允许设置为当前状态(幂等)
        }
        
        switch (from) {
            case PENDING:
                // 待处理可以转换为任何状态
                return to == ScheduleStatus.IN_PROGRESS 
                    || to == ScheduleStatus.COMPLETED 
                    || to == ScheduleStatus.CANCELLED;
                
            case IN_PROGRESS:
                // 进行中只能转换为已完成或已取消
                return to == ScheduleStatus.COMPLETED 
                    || to == ScheduleStatus.CANCELLED;
                
            case COMPLETED:
            case CANCELLED:
                // 已完成和已取消的日程不允许再流转
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * 转换为VO
     */
    private ScheduleVO convertToVO(DoctorSchedule schedule) {
        ScheduleVO vo = new ScheduleVO();
        BeanUtils.copyProperties(schedule, vo);
        
        // 设置优先级描述
        try {
            SchedulePriority priority = SchedulePriority.fromCode(schedule.getPriority());
            vo.setPriorityDesc(priority.getDescription());
        } catch (Exception e) {
            vo.setPriorityDesc("未知");
        }
        
        // 设置状态描述
        try {
            ScheduleStatus status = ScheduleStatus.fromCode(schedule.getStatus());
            vo.setStatusDesc(status.getDescription());
        } catch (Exception e) {
            vo.setStatusDesc("未知");
        }
        
        // 设置类别名称
        if (schedule.getScheduleCategory() != null) {
            ScheduleCategory category = ScheduleCategory.getByName(schedule.getScheduleCategory());
            if (category != null) {
                vo.setScheduleCategoryName(category.getDescription());
            } else {
                log.warn("Unknown schedule category: {}", schedule.getScheduleCategory());
                vo.setScheduleCategoryName("unknown");
            }
        }

        // 使用userId查询医生和患者信息
        if (schedule.getDoctorId() != null) {
            try {
                DoctorVO doctorVO = doctorClient.getDoctorInfo(GetDoctorInfoRequest.builder()
                        .userId(schedule.getDoctorId())
                        .build());
                if (doctorVO != null) {
                    vo.setDoctorName(doctorVO.getUsername());
                }
            } catch (Exception e) {
                log.warn("Failed to get doctor info for userId: {}", schedule.getDoctorId(), e);
            }
        }
        
        if (schedule.getPatientId() != null) {
            try {
                PatientVO patientVO = doctorClient.getPatientInfo(GetPatientInfoRequest.builder()
                        .userId(schedule.getPatientId())
                        .build());
                if (patientVO != null) {
                    vo.setPatientName(patientVO.getUsername());
                }
            } catch (Exception e) {
                log.warn("Failed to get patient info for userId: {}", schedule.getPatientId(), e);
            }
        }

        return vo;
    }

    /**
     * 构建诊断结果
     */
    private String buildResult(CompleteScheduleRequest request) {
        StringBuilder result = new StringBuilder();
        result.append("诊断报告: ").append(request.getDiagnosisReport());

        if (request.getPrescription() != null && !request.getPrescription().isEmpty()) {
            result.append("\n处方信息: ").append(request.getPrescription());
        }

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            result.append("\n备注: ").append(request.getNotes());
        }

        return result.toString();
    }

    /**
     * 异步生成电子病历
     *
     * 日程完成后，调用 AI 服务生成电子病历并保存到数据库
     */
    private void generateMedicalRecordAsync(DoctorSchedule schedule, CompleteScheduleRequest request) {
        try {
            // 构建请求
            GenerateMedicalRecordRequest recordRequest = new GenerateMedicalRecordRequest();
            recordRequest.setScheduleId(schedule.getId());
            recordRequest.setDoctorId(schedule.getDoctorId());
            recordRequest.setDoctorName(schedule.getDoctorName());
            recordRequest.setPatientId(schedule.getPatientId());
            recordRequest.setPatientName(schedule.getPatientName());
            recordRequest.setScheduleDay(schedule.getScheduleDay());
            recordRequest.setScheduleContent(schedule.getSchedule());

            // 设置日程类别
            ScheduleCategory category = ScheduleCategory.getByName(schedule.getScheduleCategory());
            if (category != null) {
                recordRequest.setScheduleCategory(category.getCode());
                recordRequest.setScheduleCategoryName(category.getDescription());
            }

            // 设置诊断报告
            recordRequest.setDiagnosisReport(request.getDiagnosisReport());
            recordRequest.setPrescription(request.getPrescription());
            recordRequest.setNotes(request.getNotes());
            recordRequest.setHealthReportLink(schedule.getLink());

            // 调用 AI 生成电子病历
            MedicalRecordVO recordVO = aiClient.generateMedicalRecord(recordRequest);

            if (recordVO != null) {
                // 保存电子病历到数据库
                MedicalRecord record = new MedicalRecord();
                record.setScheduleId(schedule.getId());
                record.setDoctorId(schedule.getDoctorId());
                record.setDoctorName(schedule.getDoctorName());
                record.setPatientId(schedule.getPatientId());
                record.setPatientName(schedule.getPatientName());
                record.setVisitDate(schedule.getScheduleDay());
                record.setVisitType(recordVO.getVisitType());
                record.setChiefComplaint(recordVO.getChiefComplaint());
                record.setPresentIllness(recordVO.getPresentIllness());
                record.setPastHistory(recordVO.getPastHistory());
                record.setDiagnosis(recordVO.getDiagnosis());
                record.setTreatmentPlan(recordVO.getTreatmentPlan());
                record.setPrescription(recordVO.getPrescription());
                record.setPrecautions(recordVO.getPrecautions());
                record.setFollowUpAdvice(recordVO.getFollowUpAdvice());
                record.setFullContent(recordVO.getFullContent());
                record.setCreateTime(System.currentTimeMillis());
                record.setUpdateTime(System.currentTimeMillis());

                medicalRecordMapper.insert(record);
                log.info("Medical record generated and saved, scheduleId: {}, recordId: {}",
                        schedule.getId(), record.getId());
            } else {
                log.warn("AI failed to generate medical record, scheduleId: {}", schedule.getId());
            }
        } catch (Exception e) {
            // 电子病历生成失败不影响主流程
            log.error("Failed to generate medical record for schedule: {}", schedule.getId(), e);
        }
    }
}

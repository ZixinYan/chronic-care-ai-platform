package com.zixin.healthcenterprovider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.aicapabilityapi.dto.GenerateScheduleRequest;
import com.zixin.aicapabilityapi.dto.GenerateScheduleResponse;
import com.zixin.doctorapi.enums.ScheduleCategory;
import com.zixin.doctorapi.enums.SchedulePriority;
import com.zixin.doctorapi.enums.ScheduleStatus;
import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.healthcenterapi.api.HealthReportAPI;
import com.zixin.healthcenterapi.dto.*;
import com.zixin.healthcenterapi.vo.AISummaryVO;
import com.zixin.aicapabilityapi.api.AIMedicalRecordAPI;
import com.zixin.aicapabilityapi.api.AIGlucoseReportAPI;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportRequest;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportResponse;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordRequest;
import com.zixin.thirdpartyapi.api.SMSAPI;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.healthcenterapi.enums.ReportStatus;
import com.zixin.healthcenterapi.enums.ReportType;
import com.zixin.healthcenterapi.po.HealthReport;
import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.healthcenterprovider.client.AiClient;
import com.zixin.healthcenterprovider.client.DoctorClient;
import com.zixin.healthcenterprovider.client.MessageClient;
import com.zixin.healthcenterprovider.client.UserIdentityClient;
import com.zixin.healthcenterprovider.mapper.AISummaryMapper;
import com.zixin.healthcenterprovider.mapper.HealthReportMapper;
import com.zixin.messageapi.dto.SendMessageRequest;
import com.zixin.messageapi.enums.MessageType;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.ToBCodeEnum;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.zixin.utils.constant.BizCode.HEALTHY_REPORT_JUDGEMENT;

/**
 * 健康报告服务实现
 * 
 * 核心功能:
 * 1. 报告上传(支持图片、PDF、文字)
 * 2. 报告查询(列表和详情)
 * 3. 权限控制(患者和医生)
 * 
 * @author zixin
 */
@Slf4j
@Service
@DubboService(timeout = 30000)
public class HealthReportServiceImpl implements HealthReportAPI {

    /**
     * mg/dL 转 mmol/L 的系数
     */
    private static final double MG_DL_TO_MMOL_L = 0.0555;

    /**
     * 空腹血糖阈值 (mmol/L)
     */
    private static final double FASTING_THRESHOLD = 8.3;

    /**
     * 餐后1h血糖阈值 (mmol/L)
     */
    private static final double POST_MEAL_1H_THRESHOLD = 12.7;

    /**
     * 餐后2h血糖阈值 (mmol/L)
     */
    private static final double POST_MEAL_2H_THRESHOLD = 11.1;

    /**
     * 餐后3h血糖阈值 (mmol/L)
     */
    private static final double POST_MEAL_3H_THRESHOLD = 10.0;

    private final HealthReportMapper healthReportMapper;
    private final AISummaryMapper aiSummaryMapper;
    private final MessageClient messageClient;
    private final UserIdentityClient userIdentityClient;
    private final DoctorClient doctorClient;
    private final TransactionTemplate transactionTemplate;
    private final AiClient aiClient;

    @DubboReference(check = false)
    private AIMedicalRecordAPI aiMedicalRecordAPI;

    @DubboReference(check = false)
    private AIGlucoseReportAPI aiGlucoseReportAPI;

    @DubboReference(check = false)
    private SMSAPI smsAPI;

    public HealthReportServiceImpl(HealthReportMapper healthReportMapper,
                                   AISummaryMapper aiSummaryMapper,
                                   MessageClient messageClient,
                                   UserIdentityClient userIdentityClient, DoctorClient doctorClient, TransactionTemplate transactionTemplate, AiClient aiClient) {
        this.healthReportMapper = healthReportMapper;
        this.aiSummaryMapper = aiSummaryMapper;
        this.messageClient = messageClient;
        this.userIdentityClient = userIdentityClient;
        this.doctorClient = doctorClient;
        this.transactionTemplate = transactionTemplate;
        this.aiClient = aiClient;
    }

    @Override
    public UploadReportResponse uploadReport(UploadReportRequest request) {
        StopWatch sw = new StopWatch("uploadReport");
        sw.start("参数校验");

        UploadReportResponse response = new UploadReportResponse();

        try {
            // 1. 参数校验
            if (request.getPatientId() == null) {
                // 如果没有传patientId，默认使用当前登录用户ID
                request.setPatientId(UserInfoManager.getUserId());
            }

            final PatientVO patient = userIdentityClient.getPatientInfo(request.getPatientId());
            if (patient == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者不存在");
                return response;
            }

            if (request.getReportType() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("报告类型不能为空");
                return response;
            }

            ReportType reportType = ReportType.fromCode(request.getReportType());
            if (reportType == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无效的报告类型");
                return response;
            }

            sw.stop();
            log.info("uploadReport - 参数校验完成, patientId: {}, reportType: {}",
                    request.getPatientId(), reportType.getDescription());

            // 2. 获取患者信息
            if (patient == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者信息查询失败");
                return response;
            }

            // 采用AI判断给哪个医生进行检查
            GenerateScheduleRequest generateScheduleRequest = new GenerateScheduleRequest();
            generateScheduleRequest.setScheduleDay(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            generateScheduleRequest.setBusinessRequirement(HEALTHY_REPORT_JUDGEMENT.getBizName() + "，患者：" + patient + "，报告：" + request.getDescription());
            GenerateScheduleResponse suggestSchedule = aiClient.generateSchedule(generateScheduleRequest);

            if (!ToBCodeEnum.SUCCESS.equals(suggestSchedule.getCode())
                    || suggestSchedule.getRecommendedSchedules() == null
                    || suggestSchedule.getRecommendedSchedules().isEmpty()
                    || suggestSchedule.getRecommendedSchedules().get(0).getDoctorId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage(suggestSchedule.getMessage() != null && !suggestSchedule.getMessage().isEmpty()
                        ? suggestSchedule.getMessage()
                        : "AI 排班未返回有效医生，请稍后重试");
                return response;
            }

            final DoctorVO doctor = userIdentityClient.getDoctorInfo(suggestSchedule.getRecommendedSchedules().get(0).getDoctorId());
            if (doctor == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("医生信息查询失败");
                return response;
            }

            // 3. 处理文件上传(图片或PDF类型)
            final String fileUrl = request.getFileUrl();

            // 4. 文字类型报告校验
            if (reportType == ReportType.TEXT) {
                if (request.getTextContent() == null || request.getTextContent().trim().isEmpty()) {
                    response.setCode(ToBCodeEnum.FAIL);
                    response.setMessage("文字报告内容不能为空");
                    return response;
                }
            }

            // 5. 构建排班VO
            ScheduleVO scheduleVO = buildScheduleVO(
                    patient,
                    doctor,
                    ScheduleStatus.PENDING,
                    SchedulePriority.HIGH,
                    ScheduleCategory.ONLINE_APPROVAL.getCode(),
                    ScheduleCategory.ONLINE_APPROVAL.getName(),
                    fileUrl
            );

            // 供事务内部使用的final变量

            // 6. 使用事务管理数据库操作和排班添加
            Boolean transactionResult = transactionTemplate.execute(status -> {
                try {
                    // 6.1 构建报告实体
                    HealthReport report = new HealthReport();
                    report.setPatientId(request.getPatientId());
                    report.setAttendingDoctorId(patient.getAttendingDoctorId());
                    report.setReportType(request.getReportType());
                    report.setCategory(request.getCategory());
                    report.setTitle(request.getTitle());
                    report.setDescription(request.getDescription());
                    report.setFileUrl(fileUrl);
                    report.setTextContent(request.getTextContent());
                    report.setReportDate(request.getReportDate());
                    report.setUploaderId(request.getUploaderId());
                    report.setUploaderName(patient.getNickname());
                    report.setHospitalName(request.getHospitalName());
                    report.setStatus(ReportStatus.PENDING.getCode());

                    // 6.2 保存到数据库
                    int rows = healthReportMapper.insert(report);

                    if (rows <= 0) {
                        log.error("uploadReport - 报告保存失败, patientId: {}", request.getPatientId());
                        status.setRollbackOnly();
                        response.setCode(ToBCodeEnum.FAIL);
                        response.setMessage("报告保存失败");
                        return false;
                    }

                    // 6.3 调用AI能力判断同步添加排班
                    if (patient.getAttendingDoctorId() != null) {
                        boolean scheduleSuccess = doctorClient.addSchedule(
                                doctor.getUserId(),
                                patient.getUserId(),
                                doctor.getUsername(),
                                scheduleVO
                        );

                        if (!scheduleSuccess) {
                            log.error("uploadReport - 添加排班失败, doctorId: {}", patient.getAttendingDoctorId());
                            status.setRollbackOnly();
                            response.setCode(ToBCodeEnum.FAIL);
                            response.setMessage("添加排班失败");
                            return false;
                        }

                        log.info("uploadReport - 排班添加成功, doctorId: {}", patient.getAttendingDoctorId());
                    }

                    // 6.4 设置成功响应
                    response.setCode(ToBCodeEnum.SUCCESS);
                    response.setMessage("报告上传成功");
                    response.setReportId(report.getReportId());
                    response.setFileUrl(fileUrl);

                    return true;

                } catch (Exception e) {
                    log.error("uploadReport - 事务执行异常", e);
                    status.setRollbackOnly();
                    response.setCode(ToBCodeEnum.FAIL);
                    response.setMessage("事务执行异常: " + e.getMessage());
                    return false;
                }
            });

            // 7. 事务成功后，异步发送消息（非事务性，失败不影响主流程）
            if (Boolean.TRUE.equals(transactionResult) && patient.getAttendingDoctorId() != null) {
                    // 异步发送消息
                try {
                    messageClient.sendMessageAsync(
                            patient.getUserId(),
                            SendMessageRequest.builder()
                                    .receiverId(doctor.getUserId())
                                    .messageType(MessageType.SYSTEM.getCode())
                                    .title("新健康报告上传通知")
                                    .senderName(patient.getUsername())
                                    .content("患者 " + patient.getUsername() + " 上传了新的健康报告，请及时查看。")
                                    .build()
                    );
                    log.info("uploadReport - 消息发送成功, doctorId: {}", patient.getAttendingDoctorId());
                } catch (Exception e) {
                    // 只记录日志，不影响主流程
                    log.error("uploadReport - 消息发送失败, doctorId: {}, error: {}",
                            patient.getAttendingDoctorId(), e.getMessage(), e);
                }
            }

            log.info("uploadReport - 报告上传完成, reportId: {}, patientId: {}, 耗时统计:\n{}",
                    response.getReportId(), request.getPatientId(), sw.prettyPrint());

        } catch (Exception e) {
            log.error("uploadReport - 报告上传异常, patientId: {}, error: {}",
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("报告上传异常: " + e.getMessage());
        }

        return response;
    }
    
    @Override
    public QueryReportListResponse queryReportList(QueryReportListRequest request) {
        StopWatch sw = new StopWatch("queryReportList");
        sw.start("构建查询条件");
        
        QueryReportListResponse response = new QueryReportListResponse();
        
        try {
            // 1. 参数校验
            if (request.getPatientId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID不能为空");
                return response;
            }
            
            // 2. 构建查询条件
            LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthReport::getPatientId, request.getPatientId());
            
            if (request.getReportType() != null) {
                wrapper.eq(HealthReport::getReportType, request.getReportType());
            }
            
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                wrapper.eq(HealthReport::getCategory, request.getCategory());
            }
            
            if (request.getStatus() != null) {
                wrapper.eq(HealthReport::getStatus, request.getStatus());
            }
            
            wrapper.orderByDesc(HealthReport::getCreateTime);
            
            sw.stop();
            
            // 3. 分页查询
            sw.start("执行分页查询");
            Page<HealthReport> page = new Page<>(request.getPageNum(), request.getPageSize());
            Page<HealthReport> resultPage = healthReportMapper.selectPage(page, wrapper);
            sw.stop();
            
            log.info("queryReportList - 查询完成, patientId: {}, total: {}", 
                    request.getPatientId(), resultPage.getTotal());
            
            // 4. 转换为VO
            sw.start("数据转换");
            List<HealthReportVO> voList = new ArrayList<>();
            for (HealthReport report : resultPage.getRecords()) {
                HealthReportVO vo = convertToVO(report);
                voList.add(vo);
            }
            sw.stop();
            
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setReportList(voList);
            response.setTotal(resultPage.getTotal());
            response.setPageNum(request.getPageNum());
            response.setPageSize(request.getPageSize());
            
            log.info("queryReportList - 查询成功, 耗时统计: {}", sw.prettyPrint());
            
        } catch (Exception e) {
            log.error("queryReportList - 查询异常, patientId: {}, error: {}", 
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询异常: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public GetReportDetailResponse getReportDetail(GetReportDetailRequest request) {
        GetReportDetailResponse response = new GetReportDetailResponse();
        
        try {
            // 1. 参数校验
            if (request.getReportId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("报告ID不能为空");
                return response;
            }
            
            // 2. 查询报告
            HealthReport report = healthReportMapper.selectById(request.getReportId());
            
            if (report == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("报告不存在");
                return response;
            }

            // 3. 权限校验: 只能查看自己的报告
            Long currentUserId = UserInfoManager.getUserIdOrThrow();
            if (!report.getPatientId().equals(currentUserId)) {
                log.warn("getReportDetail - 权限拒绝: userId {} 尝试查看 patientId {} 的报告 {}", 
                        currentUserId, report.getPatientId(), request.getReportId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权查看该报告");
                return response;
            }
            
            // 4. 转换为VO
            HealthReportVO vo = convertToVO(report);
            
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setReport(vo);
            
            log.info("getReportDetail - 查询成功, reportId: {}, patientId: {}", 
                    request.getReportId(), report.getPatientId());
            
        } catch (Exception e) {
            log.error("getReportDetail - 查询异常, reportId: {}, error: {}", 
                    request.getReportId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询异常: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessReportResponse processReport(ProcessReportRequest request) {
        ProcessReportResponse response = new ProcessReportResponse();
        log.info("processReport - 处理报告请求, reportId: {}, result: {}, auditMark: {}",
                request.getReportId(), request.getResult(), request.getComment());
        // 1. 获取报告详情
        HealthReport healthReport = healthReportMapper.selectById(request.getReportId());
        if (healthReport == null) {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("报告不存在");
            return response;
        }
        int version = healthReport.getVersion();
        if(!Objects.equals(healthReport.getStatus(), ReportStatus.PENDING.getCode())){
            log.warn("processReport - 报告状态异常, reportId: {}, currentStatus: {}, expectedStatus: {}",
                    request.getReportId(), healthReport.getStatus(), ReportStatus.PENDING.getCode());
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("报告已被处理");
            return response;
        }
        // 2. 更新报告状态
        int status = ReportStatus.fromCode(request.getResult()).getCode();
        healthReport.setStatus(status);
        healthReport.setAuditRemark(request.getComment());
        // 3. 乐观锁更新
        int rows = healthReportMapper.updateById(healthReport);
        if (rows <= 0) {
            log.warn("processReport - 更新失败, reportId: {}, version: {}", request.getReportId(), version);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("处理失败");
            return response;
        }
        response.setCode(ToBCodeEnum.SUCCESS);
        response.setMessage("处理成功");
        // 4. 处理完成后，发送消息通知患者
        try {
            messageClient.sendMessageAsync(
                    UserInfoManager.getUserId(),
                    SendMessageRequest.builder()
                            .receiverId(healthReport.getPatientId())
                            .messageType(MessageType.SYSTEM.getCode())
                            .title("健康报告处理结果通知")
                            .senderName(UserInfoManager.getUsername())
                            .content("您的健康报告 '" + healthReport.getTitle() + "' 已经被处理，处理结果: "
                                    + ReportStatus.fromCode(request.getResult()).getDescription()
                                    + (request.getComment() != null ? "，备注: " + request.getComment() : ""))
                            .build()
            );
            log.info("processReport - 消息发送成功, patientId: {}", healthReport.getPatientId());
        } catch (Exception e) {
            log.error("processReport - 消息发送失败, patientId: {}, error: {}",
                    healthReport.getPatientId(), e.getMessage(), e);
        }
        return response;
    }

    @Override
    public GetRecentReportsResponse getRecentReports(GetRecentReportsRequest request) {
        GetRecentReportsResponse response = new GetRecentReportsResponse();

        try {
            if (request.getPatientId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID不能为空");
                return response;
            }

            int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 5;

            // 构建查询条件
            LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthReport::getPatientId, request.getPatientId())
                    .orderByDesc(HealthReport::getCreateTime)
                    .last("LIMIT " + limit);

            List<HealthReport> reports = healthReportMapper.selectList(wrapper);

            // 转换为VO
            List<HealthReportVO> voList = new ArrayList<>();
            for (HealthReport report : reports) {
                voList.add(convertToVO(report));
            }

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setReports(voList);

            log.info("getRecentReports - 查询成功, patientId: {}, limit: {}, count: {}",
                    request.getPatientId(), limit, voList.size());

        } catch (Exception e) {
            log.error("getRecentReports - 查询异常, patientId: {}, error: {}",
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    public GetRecentAISummaryResponse getRecentAISummary(GetRecentAISummaryRequest request) {
        GetRecentAISummaryResponse response = new GetRecentAISummaryResponse();

        try {
            if (request.getPatientId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID不能为空");
                return response;
            }

            int days = request.getDays() != null && request.getDays() > 0 ? request.getDays() : 10;
            long startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);

            // 从数据库查询AI总结
            List<com.zixin.healthcenterapi.po.AISummary> aiSummaries = 
                    aiSummaryMapper.selectRecentByPatientId(request.getPatientId(), startTime);

            // 转换为VO
            List<AISummaryVO> summaries = new ArrayList<>();
            for (com.zixin.healthcenterapi.po.AISummary aiSummary : aiSummaries) {
                AISummaryVO vo = new AISummaryVO();
                vo.setSummaryId(aiSummary.getId());
                vo.setPatientId(aiSummary.getPatientId());
                vo.setSummaryDate(aiSummary.getSummaryDate());
                vo.setContent(aiSummary.getContent());
                vo.setCreateTime(aiSummary.getCreateTime());
                summaries.add(vo);
            }

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setSummaries(summaries);

            log.info("getRecentAISummary - 查询成功, patientId: {}, days: {}, count: {}",
                    request.getPatientId(), days, summaries.size());

        } catch (Exception e) {
            log.error("getRecentAISummary - 查询异常, patientId: {}, error: {}",
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    public GenerateAIReportResponse generateAIReport(GenerateAIReportRequest request) {
        GenerateAIReportResponse response = new GenerateAIReportResponse();

        try {
            if (request.getPatientId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID不能为空");
                return response;
            }

            if (request.getCurrentGlucoseValues() == null || request.getCurrentGlucoseValues().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("当前血糖数据不能为空");
                return response;
            }

            if (request.getPredictedGlucoseValues() == null || request.getPredictedGlucoseValues().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("预测血糖数据不能为空");
                return response;
            }

            // 1. 分析当前血糖情况
            GlucoseAnalysisResult currentAnalysis = analyzeCurrentGlucose(request.getCurrentGlucoseValues());

            // 2. 分析预测血糖情况
            GlucoseAnalysisResult predictedAnalysis = analyzePredictedGlucose(
                    request.getPredictedGlucoseValues(), request.getMealType());

            // 3. 检查是否触发预警
            boolean alertTriggered = predictedAnalysis.isExceeded();
            String alertMessage = null;
            if (alertTriggered) {
                alertMessage = String.format("血糖预测值%.1fmmol/L超过%s阈值%.1fmmol/L，请注意监测",
                        predictedAnalysis.getMaxValueMmol(),
                        getMealTypeDesc(request.getMealType()),
                        predictedAnalysis.getThreshold());
            }

            // 4. 调用AI服务生成健康报告（含RAG知识库支持）
            GenerateGlucoseReportRequest aiRequest = new GenerateGlucoseReportRequest();
            aiRequest.setPatientId(request.getPatientId());
            aiRequest.setCurrentGlucoseValues(request.getCurrentGlucoseValues());
            aiRequest.setPredictedGlucoseValues(request.getPredictedGlucoseValues());
            aiRequest.setMealType(request.getMealType());
            aiRequest.setMaxPredictedMmol(predictedAnalysis.getMaxValueMmol());
            aiRequest.setAvgCurrentMmol(currentAnalysis.getAvgValueMmol());
            aiRequest.setConfidence(request.getConfidence());
            aiRequest.setAlertTriggered(alertTriggered);

            GenerateGlucoseReportResponse aiReportResponse = aiGlucoseReportAPI.generateHealthReport(aiRequest);

            String reportTitle;
            String reportContent;
            String healthSuggestions;
            if (ToBCodeEnum.SUCCESS.equals(aiReportResponse.getCode())) {
                reportTitle = aiReportResponse.getReportTitle();
                reportContent = aiReportResponse.getFullReportContent();
                healthSuggestions = aiReportResponse.getHealthSuggestions();
                log.info("generateAIReport - AI报告生成成功, patientId: {}", request.getPatientId());
            } else {
                log.warn("generateAIReport - AI报告生成失败，降级为模板报告, patientId: {}, msg: {}",
                        request.getPatientId(), aiReportResponse.getMessage());
                reportTitle = buildReportTitle(currentAnalysis, predictedAnalysis);
                reportContent = buildComprehensiveReport(request, currentAnalysis, predictedAnalysis);
                healthSuggestions = buildReportSummary(currentAnalysis, predictedAnalysis, alertTriggered);
            }

            // 5. 调用uploadReport接口上传报告（入库 + 智能排班给医生审核）
            UploadReportRequest uploadRequest = new UploadReportRequest();
            uploadRequest.setPatientId(request.getPatientId());
            uploadRequest.setUploaderId(request.getPatientId());
            uploadRequest.setReportType(2); // 文字报告类型
            uploadRequest.setCategory("GLUCOSE_PREDICTION");
            uploadRequest.setTitle(reportTitle);
            uploadRequest.setDescription(healthSuggestions);
            uploadRequest.setTextContent(reportContent);
            uploadRequest.setReportDate(java.time.LocalDate.now().toString());

            UploadReportResponse uploadResponse = this.uploadReport(uploadRequest);

            if (!ToBCodeEnum.SUCCESS.equals(uploadResponse.getCode())) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("报告上传失败: " + uploadResponse.getMessage());
                return response;
            }

            // 6. 如果触发预警，发送短信
            if (alertTriggered) {
                sendGlucoseAlertSMS(request.getPatientId(),
                        predictedAnalysis.getMaxValueMmol(), request.getMealType());
            }

            // 7. 构建响应
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("报告生成并上传成功");
            response.setReportId(uploadResponse.getReportId());
            response.setAlertTriggered(alertTriggered);
            response.setAlertMessage(alertMessage);

            log.info("generateAIReport - 报告生成并上传成功, patientId: {}, reportId: {}, alertTriggered: {}",
                    request.getPatientId(), uploadResponse.getReportId(), alertTriggered);

        } catch (Exception e) {
            log.error("generateAIReport - 报告生成异常, patientId: {}, error: {}",
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("报告生成异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    public CheckGlucoseAlertResponse checkGlucoseAlert(CheckGlucoseAlertRequest request) {
        CheckGlucoseAlertResponse response = new CheckGlucoseAlertResponse();

        try {
            if (request.getPatientId() == null || request.getCbgValue() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID和CBG值不能为空");
                return response;
            }

            // 1. 单位转换
            double cbgMmol = request.getCbgValue() * MG_DL_TO_MMOL_L;

            // 2. 获取阈值
            double threshold = getThresholdByMealType(request.getMealType());

            // 3. 判断是否超过阈值
            boolean exceeded = cbgMmol > threshold;

            // 4. 如果超过阈值，发送短信
            boolean smsSent = false;
            if (exceeded) {
                smsSent = sendGlucoseAlertSMS(request.getPatientId(), cbgMmol, request.getMealType());
            }

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("检查完成");
            response.setExceeded(exceeded);
            response.setCbgMmol(cbgMmol);
            response.setThreshold(threshold);
            response.setSmsSent(smsSent);

            log.info("checkGlucoseAlert - 检查完成, patientId: {}, cbg: {}mmol/L, threshold: {}mmol/L, exceeded: {}, smsSent: {}",
                    request.getPatientId(), cbgMmol, threshold, exceeded, smsSent);

        } catch (Exception e) {
            log.error("checkGlucoseAlert - 检查异常, patientId: {}, error: {}",
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("检查异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    public SaveTextReportResponse saveTextReport(SaveTextReportRequest request) {
        SaveTextReportResponse response = new SaveTextReportResponse();

        try {
            if (request.getPatientId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID不能为空");
                return response;
            }

            if (request.getTextContent() == null || request.getTextContent().trim().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("报告内容不能为空");
                return response;
            }

            HealthReport report = new HealthReport();
            report.setPatientId(request.getPatientId());
            report.setUploaderId(request.getUploaderId());
            report.setReportType(ReportType.TEXT.getCode());
            report.setCategory(request.getCategory());
            report.setTitle(request.getTitle());
            report.setTextContent(request.getTextContent());
            report.setReportDate(request.getReportDate());
            report.setStatus(ReportStatus.PENDING.getCode());

            int rows = healthReportMapper.insert(report);
            if (rows <= 0) {
                log.error("saveTextReport - 报告保存失败, patientId: {}", request.getPatientId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("报告保存失败");
                return response;
            }

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("报告保存成功");
            response.setReportId(report.getReportId());

            log.info("saveTextReport - 保存成功, reportId: {}, patientId: {}",
                    report.getReportId(), request.getPatientId());

        } catch (Exception e) {
            log.error("saveTextReport - 保存异常, patientId: {}, error: {}",
                    request.getPatientId(), e.getMessage(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("报告保存异常: " + e.getMessage());
        }

        return response;
    }

    /**
     * 根据用餐类型获取阈值
     */
    private double getThresholdByMealType(Integer mealType) {
        if (mealType == null) {
            return FASTING_THRESHOLD;
        }
        return switch (mealType) {
            case 1 -> FASTING_THRESHOLD;
            case 2 -> POST_MEAL_1H_THRESHOLD;
            case 3 -> POST_MEAL_2H_THRESHOLD;
            case 4 -> POST_MEAL_3H_THRESHOLD;
            default -> FASTING_THRESHOLD;
        };
    }

    /**
     * 获取用餐类型描述
     */
    private String getMealTypeDesc(Integer mealType) {
        if (mealType == null) {
            return "空腹";
        }
        return switch (mealType) {
            case 1 -> "空腹";
            case 2 -> "餐后1小时";
            case 3 -> "餐后2小时";
            case 4 -> "餐后3小时";
            default -> "未知";
        };
    }

    /**
     * 发送血糖预警短信
     */
    private boolean sendGlucoseAlertSMS(Long patientId, double cbgMmol, Integer mealType) {
        try {
            // 1. 获取患者信息
            PatientVO patient = userIdentityClient.getPatientInfo(patientId);
            if (patient == null) {
                log.warn("sendGlucoseAlertSMS - 患者不存在, patientId: {}", patientId);
                return false;
            }

            // 2. 获取紧急联系人电话
            String emergencyPhone = patient.getEmergencyPhone();
            if (emergencyPhone == null || emergencyPhone.isEmpty()) {
                log.warn("sendGlucoseAlertSMS - 未设置紧急联系人电话, patientId: {}", patientId);
                return false;
            }

            // 3. 构建短信内容
            String mealTypeDesc = getMealTypeDesc(mealType);
            String smsContent = String.format(
                "【慢病管理平台】预警：患者%s的%s血糖预测值为%.1fmmol/L，超过正常阈值，请及时关注。",
                patient.getNickname() != null ? patient.getNickname() : "",
                mealTypeDesc,
                cbgMmol
            );

            // 4. 发送短信【TODO: 使用实际短信模板ID】
            SendSMSRequest smsRequest = new SendSMSRequest();
            smsRequest.setPhone(emergencyPhone);
            smsRequest.setCode(smsContent);
            smsRequest.setTemplateId("GLUCOSE_ALERT"); // 【TODO: 替换为实际模板ID】

            SendSMSResponse smsResponse = smsAPI.sendSMS(smsRequest);

            if (smsResponse.getCode() == ToBCodeEnum.SUCCESS) {
                log.info("sendGlucoseAlertSMS - 短信发送成功, patientId: {}, phone: {}",
                        patientId, emergencyPhone);
                return true;
            } else {
                log.warn("sendGlucoseAlertSMS - 短信发送失败, patientId: {}, error: {}",
                        patientId, smsResponse.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("sendGlucoseAlertSMS - 发送短信异常, patientId: {}", patientId, e);
            return false;
        }
    }

    /**
     * 构建患者数据JSON
     */
    private String buildPatientDataJson(GenerateAIReportRequest request) {
        // 【TODO: 根据实际需要使用更完善的JSON构建方式】
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"predictedGlucose\":").append(request.getPredictedGlucoseValues()).append(",");
        sb.append("\"mealType\":").append(request.getMealType()).append(",");
        sb.append("\"predictStartTime\":").append(request.getPredictStartTime());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 分析当前血糖数据
     */
    private GlucoseAnalysisResult analyzeCurrentGlucose(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new GlucoseAnalysisResult(0, 0, 0, false);
        }

        double sum = 0;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        for (Double value : values) {
            sum += value;
            max = Math.max(max, value);
            min = Math.min(min, value);
        }

        double avg = sum / values.size();
        return new GlucoseAnalysisResult(avg, max, min, false);
    }

    /**
     * 分析预测血糖数据
     */
    private GlucoseAnalysisResult analyzePredictedGlucose(List<Double> values, Integer mealType) {
        if (values == null || values.isEmpty()) {
            return new GlucoseAnalysisResult(0, 0, 0, false);
        }

        double threshold = getThresholdByMealType(mealType);
        double sum = 0;
        double max = Double.MIN_VALUE;
        double maxMmol = 0;
        double min = Double.MAX_VALUE;
        boolean exceeded = false;

        for (Double value : values) {
            double mmol = value * MG_DL_TO_MMOL_L;
            sum += mmol;
            max = Math.max(max, value);
            maxMmol = Math.max(maxMmol, mmol);
            min = Math.min(min, value);

            if (mmol > threshold) {
                exceeded = true;
            }
        }

        double avg = sum / values.size();
        return new GlucoseAnalysisResult(avg, max, min, maxMmol, exceeded, threshold, mealType);
    }

    /**
     * 构建报告标题
     */
    private String buildReportTitle(GlucoseAnalysisResult current, GlucoseAnalysisResult predicted) {
        StringBuilder title = new StringBuilder();
        title.append("血糖监测与预测报告 - ");

        if (predicted.isExceeded()) {
            title.append(String.format("【预警】预测最高%.1fmmol/L", predicted.getMaxValueMmol()));
        } else {
            title.append("血糖趋势正常");
        }

        return title.toString();
    }

    /**
     * 构建报告摘要
     */
    private String buildReportSummary(GlucoseAnalysisResult current,
                                       GlucoseAnalysisResult predicted, boolean alertTriggered) {
        StringBuilder summary = new StringBuilder();

        // 当前血糖概况
        summary.append(String.format("当前血糖平均值%.1fmmol/L（%.0fmg/dL），", 
                current.getAvgValueMmol(), current.getAvgValue()));
        summary.append(String.format("最高%.1fmmol/L，最低%.1fmmol/L。",
                current.getMaxValueMmol(), current.getMinValueMmol()));

        // 预测概况
        summary.append(String.format("预测未来血糖平均值%.1fmmol/L，", predicted.getAvgValueMmol()));
        summary.append(String.format("最高可达%.1fmmol/L。", predicted.getMaxValueMmol()));

        // 预警信息
        if (alertTriggered) {
            summary.append(String.format("【预警】预测血糖将超过%s阈值%.1fmmol/L，建议密切关注。",
                    getMealTypeDesc(predicted.getMealType()), predicted.getThreshold()));
        } else {
            summary.append("预测血糖在正常范围内，继续保持良好的血糖管理。");
        }

        return summary.toString();
    }

    /**
     * 构建综合报告内容
     */
    private String buildComprehensiveReport(GenerateAIReportRequest request,
                                            GlucoseAnalysisResult current,
                                            GlucoseAnalysisResult predicted) {
        StringBuilder content = new StringBuilder();

        // 报告标题
        content.append("# 血糖监测与预测分析报告\n\n");

        // 生成时间
        content.append("**报告生成时间**: ").append(new java.util.Date()).append("\n\n");

        // 一、当前血糖情况
        content.append("## 一、当前血糖监测情况\n\n");
        content.append("### 1. 统计指标\n\n");
        content.append(String.format("- **平均值**: %.2f mg/dL (%.2f mmol/L)\n",
                current.getAvgValue(), current.getAvgValueMmol()));
        content.append(String.format("- **最高值**: %.2f mg/dL (%.2f mmol/L)\n",
                current.getMaxValue(), current.getMaxValueMmol()));
        content.append(String.format("- **最低值**: %.2f mg/dL (%.2f mmol/L)\n",
                current.getMinValue(), current.getMinValueMmol()));
        content.append(String.format("- **监测点数**: %d\n\n",
                request.getCurrentGlucoseValues().size()));

        // 原始数据
        content.append("### 2. 原始监测数据\n\n");
        content.append("| 时间 | 血糖值 (mg/dL) | 血糖值 (mmol/L) |\n");
        content.append("|------|----------------|------------------|\n");
        for (int i = 0; i < Math.min(request.getCurrentGlucoseValues().size(), 20); i++) {
            Double value = request.getCurrentGlucoseValues().get(i);
            String time = request.getCurrentGlucoseTimes() != null && i < request.getCurrentGlucoseTimes().size()
                    ? new java.util.Date(request.getCurrentGlucoseTimes().get(i)).toString()
                    : "时间点" + (i + 1);
            content.append(String.format("| %s | %.2f | %.2f |\n",
                    time, value, value * MG_DL_TO_MMOL_L));
        }
        if (request.getCurrentGlucoseValues().size() > 20) {
            content.append("| ... | ... | ... |\n");
        }
        content.append("\n");

        // 二、血糖预测结果
        content.append("## 二、血糖预测结果\n\n");
        content.append("### 1. 预测统计\n\n");
        content.append(String.format("- **用餐类型**: %s\n", getMealTypeDesc(request.getMealType())));
        content.append(String.format("- **预测时长**: %d 小时\n",
                request.getPredictedGlucoseValues().size() / 12));
        content.append(String.format("- **预测平均值**: %.2f mg/dL (%.2f mmol/L)\n",
                predicted.getAvgValue(), predicted.getAvgValueMmol()));
        content.append(String.format("- **预测最高值**: %.2f mg/dL (%.2f mmol/L)\n",
                predicted.getMaxValue(), predicted.getMaxValueMmol()));
        content.append(String.format("- **预测最低值**: %.2f mg/dL (%.2f mmol/L)\n",
                predicted.getMinValue(), predicted.getMinValueMmol()));
        content.append(String.format("- **预测置信度**: %.0f%%\n\n",
                request.getConfidence() != null ? request.getConfidence() * 100 : 85));

        // 预测阈值判断
        content.append("### 2. 阈值检测\n\n");
        double threshold = predicted.getThreshold();
        content.append(String.format("- **当前阈值**: %.1f mmol/L (%s)\n",
                threshold, getMealTypeDesc(request.getMealType())));

        if (predicted.isExceeded()) {
            content.append("- **检测结果**: ⚠️ **超标预警**\n");
            content.append(String.format("  - 预测最高值 %.1f mmol/L 超过阈值 %.1f mmol/L\n",
                    predicted.getMaxValueMmol(), threshold));
            content.append("  - 建议采取干预措施，密切监测血糖变化\n\n");
        } else {
            content.append("- **检测结果**: ✅ **正常**\n");
            content.append(String.format("  - 预测最高值 %.1f mmol/L 未超过阈值 %.1f mmol/L\n\n",
                    predicted.getMaxValueMmol(), threshold));
        }

        // 预测数据表格
        content.append("### 3. 预测数据明细\n\n");
        content.append("| 预测时间 | 血糖值 (mg/dL) | 血糖值 (mmol/L) | 状态 |\n");
        content.append("|----------|----------------|------------------|------|\n");
        for (int i = 0; i < request.getPredictedGlucoseValues().size(); i++) {
            Double value = request.getPredictedGlucoseValues().get(i);
            String time = request.getPredictedTimes() != null && i < request.getPredictedTimes().size()
                    ? new java.util.Date(request.getPredictedTimes().get(i)).toString()
                    : "+" + ((i + 1) * 5) + "分钟";
            double mmol = value * MG_DL_TO_MMOL_L;
            String status = mmol > threshold ? "⚠️ 超标" : "✅ 正常";
            content.append(String.format("| %s | %.2f | %.2f | %s |\n", time, value, mmol, status));
        }
        content.append("\n");

        // 三、综合分析与建议
        content.append("## 三、综合分析与建议\n\n");

        // 趋势分析
        content.append("### 1. 趋势分析\n\n");
        double trend = predicted.getAvgValue() - current.getAvgValue();
        if (Math.abs(trend) < 5) {
            content.append("- 血糖预测趋势**平稳**，预计维持在当前水平\n");
        } else if (trend > 0) {
            content.append(String.format("- 血糖预测呈**上升趋势**，预计上升约 %.1f mg/dL\n", trend));
        } else {
            content.append(String.format("- 血糖预测呈**下降趋势**，预计下降约 %.1f mg/dL\n", Math.abs(trend)));
        }

        // 基于用餐类型的建议
        content.append("\n### 2. 健康建议\n\n");
        if (predicted.isExceeded()) {
            content.append("⚠️ **预警建议**：\n\n");
            content.append("- 建议立即监测血糖，确认实际情况\n");
            content.append("- 如血糖持续升高，请及时就医\n");
            content.append("- 检查胰岛素注射量是否充足\n");
            content.append("- 避免高糖食物摄入\n");
            content.append("- 家属已收到预警短信通知\n");
        } else {
            content.append("✅ **维持建议**：\n\n");
            content.append("- 当前血糖管理良好，请继续保持\n");
            switch (request.getMealType()) {
                case 1 -> content.append("- 空腹血糖正常，建议按时进食早餐\n");
                case 2, 3, 4 -> content.append("- 餐后血糖控制良好，继续保持规律饮食\n");
            }
            content.append("- 建议定期监测血糖变化\n");
        }

        // 数据维度信息
        if (request.getDataDimensions() != null) {
            content.append("\n## 四、监测数据维度\n\n");
            GlucoseDataDimensions dim = request.getDataDimensions();
            if (dim.getCbg() != null) content.append("- **CGM数据**: " + dim.getCbg().size() + " 条\n");
            if (dim.getFinger() != null) content.append("- **指尖血数据**: " + dim.getFinger().size() + " 条\n");
            if (dim.getBasal() != null) content.append("- **基础率数据**: " + dim.getBasal().size() + " 条\n");
            if (dim.getHr() != null) content.append("- **心率数据**: " + dim.getHr().size() + " 条\n");
            if (dim.getGsr() != null) content.append("- **皮肤电反应数据**: " + dim.getGsr().size() + " 条\n");
            if (dim.getCarbInput() != null) content.append("- **碳水摄入数据**: " + dim.getCarbInput().size() + " 条\n");
            if (dim.getBolus() != null) content.append("- **大剂量胰岛素数据**: " + dim.getBolus().size() + " 条\n");
        }

        // 报告结尾
        content.append("\n---\n\n");
        content.append("*本报告由AI基于监测数据自动生成，仅供参考。如有疑问请咨询医生。*\n");

        return content.toString();
    }

    /**
     * 血糖分析结果内部类
     */
    private static class GlucoseAnalysisResult {
        private final double avgValue;
        private final double maxValue;
        private final double minValue;
        private final double maxValueMmol;
        private final boolean exceeded;
        private final double threshold;
        private final Integer mealType;

        public GlucoseAnalysisResult(double avg, double max, double min, boolean exceeded) {
            this.avgValue = avg;
            this.maxValue = max;
            this.minValue = min;
            this.maxValueMmol = max * 0.0555;
            this.exceeded = exceeded;
            this.threshold = 0;
            this.mealType = 1;
        }

        public GlucoseAnalysisResult(double avgMmol, double max, double min,
                                      double maxMmol, boolean exceeded, double threshold, Integer mealType) {
            this.avgValue = avgMmol / 0.0555;
            this.maxValue = max;
            this.minValue = min;
            this.maxValueMmol = maxMmol;
            this.exceeded = exceeded;
            this.threshold = threshold;
            this.mealType = mealType != null ? mealType : 1;
        }

        public double getAvgValue() { return avgValue; }
        public double getAvgValueMmol() { return avgValue * 0.0555; }
        public double getMaxValue() { return maxValue; }
        public double getMaxValueMmol() { return maxValueMmol; }
        public double getMinValue() { return minValue; }
        public double getMinValueMmol() { return minValue * 0.0555; }
        public boolean isExceeded() { return exceeded; }
        public double getThreshold() { return threshold; }
        public Integer getMealType() { return mealType; }
    }

    /**
     * 将实体转换为VO
     */
    private HealthReportVO convertToVO(HealthReport report) {
        HealthReportVO vo = new HealthReportVO();
        BeanUtils.copyProperties(report, vo);
        
        // 设置报告类型描述
        ReportType reportType = ReportType.fromCode(report.getReportType());
        if (reportType != null) {
            vo.setReportTypeDesc(reportType.getDescription());
        }
        
        // 设置审核状态描述
        ReportStatus status = ReportStatus.fromCode(report.getStatus());
        if (status != null) {
            vo.setStatusDesc(status.getDescription());
        }
        
        // 查询患者姓名(使用userId)
        if (report.getPatientId() != null) {
            try {
                PatientVO patient = userIdentityClient.getPatientInfo(report.getPatientId());
                if (patient != null) {
                    vo.setPatientName(patient.getNickname());
                }
            } catch (Exception e) {
                log.warn("Failed to get patient name, patientId: {}", report.getPatientId(), e);
            }
        }
        
        // 查询医生姓名(使用userId)
        if (report.getAttendingDoctorId() != null) {
            try {
                com.zixin.accountapi.vo.DoctorVO doctor = userIdentityClient.getDoctorInfo(report.getAttendingDoctorId());
                if (doctor != null) {
                    vo.setDoctorName(doctor.getNickname());
                }
            } catch (Exception e) {
                log.warn("Failed to get doctor name, doctorId: {}", report.getAttendingDoctorId(), e);
            }
        }
        
        return vo;
    }

    private ScheduleVO buildScheduleVO(PatientVO patient, DoctorVO doctor,
                                       ScheduleStatus status,
                                       SchedulePriority priority,
                                       Integer category,
                                       String categoryName,
                                       String link) {
        ScheduleVO scheduleVO = new ScheduleVO();

        // 基础信息
        scheduleVO.setSchedule("查看患者 " + patient.getUsername() + " 的诊断报告");
        scheduleVO.setDoctorId(doctor.getUserId());
        scheduleVO.setDoctorName(doctor.getUsername());
        scheduleVO.setPatientId(patient.getUserId());
        scheduleVO.setPatientName(patient.getUsername());

        // 状态设置
        scheduleVO.setStatus(status.getCode());
        scheduleVO.setStatusDesc(status.getDescription());

        // 优先级设置
        scheduleVO.setPriority(priority.getCode());
        scheduleVO.setPriorityDesc(priority.getDescription());

        // 分类设置
        scheduleVO.setScheduleCategory(category);
        scheduleVO.setScheduleCategoryName(categoryName);

        scheduleVO.setLink(link);

        // 时间设置（默认今天0点到明天0点）
        setDefaultTimeRange(scheduleVO);

        return scheduleVO;
    }

    private void setDefaultTimeRange(ScheduleVO scheduleVO) {
        LocalDate today = LocalDate.now();
        scheduleVO.setScheduleDay(today.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        scheduleVO.setStartTime(today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        scheduleVO.setEndTime(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}

package com.zixin.healthcenterconsumer.controller;

import com.zixin.healthcenterapi.api.HealthReportAPI;
import com.zixin.healthcenterapi.dto.*;
import com.zixin.healthcenterapi.vo.AISummaryVO;
import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.healthcenterconsumer.client.OSSClient;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.BusinessException;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.security.RequirePermission;
import com.zixin.utils.security.RequireRole;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 健康报告控制器
 * 
 * 提供健康报告的上传、查询等HTTP接口
 * 
 * 权限要求:
 * - 患者可上传和查看自己的报告
 * - 医生可查看自己患者的报告
 * 
 * @author zixin
 */
@RestController
@RequestMapping("/health/report")
@Slf4j
public class HealthReportController {
    
    @DubboReference(check = false)
    private HealthReportAPI healthReportAPI;
    
    /**
     * 允许的图片文件类型
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/jpg"
    );
    
    /**
     * 允许的PDF文件类型
     */
    private static final List<String> ALLOWED_PDF_TYPES = Arrays.asList(
            "application/pdf"
    );
    
    /**
     * 文件大小限制 (10MB)
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final OSSClient ossClient;

    public HealthReportController(OSSClient ossClient) {
        this.ossClient = ossClient;
    }

    /**
     * 上传健康报告
     * 支持三种类型:
     * 1. 图片报告 (reportType=1, 需要file参数)
     * 2. 文字报告 (reportType=2, 需要textContent参数)
     * 3. PDF报告 (reportType=3, 需要file参数)
     * @return 上传结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole("PATIENT")
    public Result<UploadReportResponse> uploadReport(
            @ModelAttribute  UploadReportRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Long currentUserId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();
        String clientIp = UserInfoManager.getRequestIp();

        request.setUploaderId(UserInfoManager.getUserId());
        log.info("uploadReport - userId: {}, patientId: {}, reportType: {}, title: {}, ip: {}, traceId: {}", 
                currentUserId, request.getPatientId(), request.getReportType(), request.getTitle(), clientIp, traceId);
        
        // 1. 文件上传
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            validateFile(file, request.getReportType());
            fileUrl = ossClient.uploadFile(file);
        }

        if(fileUrl == null && (request.getReportType() == 1 || request.getReportType() == 3)) {
            log.error("uploadReport - 文件上传失败, reportType: {}, title: {}, traceId: {}",
                    request.getReportType(), request.getTitle(), traceId);
            throw new BusinessException("文件上传失败");
        }
        request.setFileUrl(fileUrl);

        // 2. 调用Dubbo服务
        UploadReportResponse response = healthReportAPI.uploadReport(request);
        
        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 保存文字报告
     * 
     * @param request 文字报告请求
     * @return 保存结果
     */
    @PostMapping("/save-text")
    @RequireRole("PATIENT")
    public Result<SaveTextReportResponse> saveTextReport(@RequestBody SaveTextReportRequest request) {
        Long currentUserId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        request.setUploaderId(currentUserId);
        request.setPatientId(currentUserId);
        
        log.info("saveTextReport - userId: {}, title: {}, category: {}, traceId: {}",
                currentUserId, request.getTitle(), request.getCategory(), traceId);

        SaveTextReportResponse response = healthReportAPI.saveTextReport(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }
    
    /**
     * 查询健康报告列表
     * 
     * 权限要求:
     * - 需要PATIENT角色
     * - 需要health:report:read权限
     * - 患者只能查看自己的报告
     *
     * @param reportType 报告类型 (可选)
     * @param category 报告分类 (可选)
     * @param status 审核状态 (可选)
     * @param pageNum 页码 (默认1)
     * @param pageSize 每页数量 (默认10)
     * @return 报告列表
     */
    @GetMapping("/list")
    @RequireRole("PATIENT")
    public Result<QueryReportListResponse> queryReportList(
            @RequestParam(required = false, value = "reportType") Integer reportType,
            @RequestParam(required = false, value = "category") String category,
            @RequestParam(required = false, value = "status") Integer status,
            @RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum,
            @RequestParam(defaultValue = "10", value = "pageSize") Integer pageSize) {
        
        // 从ThreadLocal获取当前用户信息
        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();
        
        log.info("queryReportList - patientId: {}, pageNum: {}, pageSize: {}, traceId: {}"
                , patientId, pageNum, pageSize, traceId);

        
        // 构建请求
        QueryReportListRequest queryRequest = new QueryReportListRequest();
        queryRequest.setPatientId(patientId);
        queryRequest.setReportType(reportType);
        queryRequest.setCategory(category);
        queryRequest.setStatus(status);
        queryRequest.setPageNum(pageNum);
        queryRequest.setPageSize(pageSize);
        
        // 调用Dubbo服务
        QueryReportListResponse response = healthReportAPI.queryReportList(queryRequest);
        
        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }
    
    /**
     * 获取报告详情
     *
     * 权限要求:
     * - 需要PATIENT角色
     * - 只能查看自己的报告详情
     *
     * @param reportId 报告ID
     * @return 报告详情
     */
    @GetMapping("/detail")
    @RequireRole({"PATIENT","DOCTOR"})
    public Result<GetReportDetailResponse> getReportDetail(@RequestParam(value = "reportId") Long reportId) {
        // 从ThreadLocal获取当前用户信息
        Long currentUserId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("getReportDetail - userId: {}, reportId: {}, traceId: {}",
                currentUserId, reportId, traceId);

        // 构建请求
        GetReportDetailRequest detailRequest = new GetReportDetailRequest();
        detailRequest.setReportId(reportId);

        // 调用Dubbo服务
        GetReportDetailResponse response = healthReportAPI.getReportDetail(detailRequest);

        if ("SUCCESS".equals(response.getCode().name())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 获取最近N次健康报告
     *
     * 权限要求:
     * - 需要PATIENT角色
     * - 患者只能查看自己的报告
     *
     * @param limit 获取数量（默认5）
     * @return 最近报告列表
     */
    @GetMapping("/recent")
    @RequireRole("PATIENT")
    public Result<GetRecentReportsResponse> getRecentReports(
            @RequestParam(defaultValue = "5", value = "limit") Integer limit) {
        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("getRecentReports - patientId: {}, limit: {}, traceId: {}",
                patientId, limit, traceId);

        GetRecentReportsRequest request = new GetRecentReportsRequest();
        request.setPatientId(patientId);
        request.setLimit(limit);

        GetRecentReportsResponse response = healthReportAPI.getRecentReports(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 获取最近N天AI总结信息
     *
     * 权限要求:
     * - 需要PATIENT角色
     * - 患者只能查看自己的总结
     *
     * @param days 查询天数（默认10）
     * @return AI总结列表
     */
    @GetMapping("/ai-summary")
    @RequireRole("PATIENT")
    public Result<GetRecentAISummaryResponse> getRecentAISummary(
            @RequestParam(defaultValue = "10", value = "days") Integer days) {
        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("getRecentAISummary - patientId: {}, days: {}, traceId: {}",
                patientId, days, traceId);

        GetRecentAISummaryRequest request = new GetRecentAISummaryRequest();
        request.setPatientId(patientId);
        request.setDays(days);

        GetRecentAISummaryResponse response = healthReportAPI.getRecentAISummary(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 获取AI推荐医生列表
     *
     * 权限要求:
     * - 需要PATIENT角色
     * - 只能为自己待审核的报告获取推荐医生
     *
     * @param reportId 报告ID
     * @return 推荐医生列表
     */
    @GetMapping("/recommended-doctors")
    @RequireRole("PATIENT")
    public Result<GetRecommendedDoctorsResponse> getRecommendedDoctors(
            @RequestParam(value = "reportId") Long reportId) {
        Long currentUserId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("getRecommendedDoctors - userId: {}, reportId: {}, traceId: {}",
                currentUserId, reportId, traceId);

        GetRecommendedDoctorsRequest request = new GetRecommendedDoctorsRequest();
        request.setReportId(reportId);

        GetRecommendedDoctorsResponse response = healthReportAPI.getRecommendedDoctors(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 发送报告给医生
     *
     * 权限要求:
     * - 需要PATIENT角色
     * - 只能发送自己待审核的报告
     *
     * @param request 发送请求（包含reportId和doctorId）
     * @return 发送结果
     */
    @PostMapping("/send-to-doctor")
    @RequireRole("PATIENT")
    public Result<SendReportToDoctorResponse> sendReportToDoctor(@RequestBody SendReportToDoctorRequest request) {
        Long currentUserId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("sendReportToDoctor - userId: {}, reportId: {}, doctorId: {}, traceId: {}",
                currentUserId, request.getReportId(), request.getDoctorId(), traceId);

        SendReportToDoctorResponse response = healthReportAPI.sendReportToDoctor(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 医生查询待审批报告列表
     *
     * 权限要求:
     * - 需要DOCTOR角色
     * - 只能查看发送给自己的待审批报告
     *
     * @param reportType 报告类型 (可选)
     * @param category 报告分类 (可选)
     * @param pageNum 页码 (默认1)
     * @param pageSize 每页数量 (默认10)
     * @return 待审批报告列表
     */
    @GetMapping("/pending-approval")
    @RequireRole("DOCTOR")
    public Result<QueryPendingReportsResponse> queryPendingReports(
            @RequestParam(required = false, value = "reportType") Integer reportType,
            @RequestParam(required = false, value = "category") String category,
            @RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum,
            @RequestParam(defaultValue = "10", value = "pageSize") Integer pageSize) {

        Long doctorId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("queryPendingReports - doctorId: {}, pageNum: {}, pageSize: {}, traceId: {}",
                doctorId, pageNum, pageSize, traceId);

        QueryPendingReportsRequest request = new QueryPendingReportsRequest();
        request.setDoctorId(doctorId);
        request.setReportType(reportType);
        request.setCategory(category);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);

        QueryPendingReportsResponse response = healthReportAPI.queryPendingReports(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }
    
    /**
     * 文件安全校验
     * 
     * 校验内容:
     * 1. 文件大小不超过10MB
     * 2. 文件类型必须在白名单中
     * 3. 文件后缀合法
     * 
     * @param file 上传的文件
     * @param reportType 报告类型
     * @throws BusinessException 校验失败抛出异常
     */
    private void validateFile(MultipartFile file, Integer reportType) {
        // 1. 文件大小校验 (10MB)
        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("validateFile - 文件过大: {} bytes, 限制: {} bytes", file.getSize(), MAX_FILE_SIZE);
            throw new BusinessException("文件大小不能超过10MB");
        }
        
        // 2. 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null) {
            log.error("validateFile - 文件类型为空");
            throw new BusinessException("无法识别文件类型");
        }
        
        // 根据reportType校验文件类型
        if (reportType == 1) {  // 图片类型
            if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                log.error("validateFile - 不支持的图片类型: {}", contentType);
                throw new BusinessException("只支持PNG、JPG、JPEG格式的图片");
            }
        } else if (reportType == 3) {  // PDF类型
            if (!ALLOWED_PDF_TYPES.contains(contentType.toLowerCase())) {
                log.error("validateFile - 不支持的PDF类型: {}", contentType);
                throw new BusinessException("只支持PDF格式的文档");
            }
        }
        
        // 3. 文件后缀校验
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            
            if (reportType == 1) {  // 图片类型
                if (!lowerFilename.matches(".*\\.(png|jpg|jpeg)$")) {
                    log.error("validateFile - 图片文件后缀不合法: {}", filename);
                    throw new BusinessException("图片文件后缀必须是png、jpg或jpeg");
                }
            } else if (reportType == 3) {  // PDF类型
                if (!lowerFilename.endsWith(".pdf")) {
                    log.error("validateFile - PDF文件后缀不合法: {}", filename);
                    throw new BusinessException("PDF文件后缀必须是.pdf");
                }
            }
        }
        
        log.info("validateFile - 文件校验通过: filename={}, contentType={}, size={} bytes", 
                filename, contentType, file.getSize());
    }
}

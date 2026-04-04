package com.zixin.aicapabilityprovider.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zixin.accountapi.api.UserIdentityAPI;
import com.zixin.accountapi.dto.GetPatientInfoResponse;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.healthcenterapi.api.HealthReportAPI;
import com.zixin.healthcenterapi.dto.QueryReportListRequest;
import com.zixin.healthcenterapi.dto.QueryReportListResponse;
import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.doctorapi.api.DoctorWorkbenchAPI;
import com.zixin.doctorapi.dto.QueryScheduleRequest;
import com.zixin.doctorapi.dto.QueryScheduleResponse;
import com.zixin.doctorapi.vo.ScheduleVO;
import com.zixin.utils.utils.PageUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 电子病历生成工具类
 *
 * 提供查询患者健康数据、历史病历等信息的工具
 */
@Configuration
@Slf4j
public class MedRecordTools {

    private final Gson gson = new GsonBuilder().create();

    @DubboReference(timeout = 50000)
    private UserIdentityAPI userIdentityAPI;

    @DubboReference(timeout = 50000)
    private HealthReportAPI healthReportAPI;

    @DubboReference(timeout = 50000)
    private DoctorWorkbenchAPI doctorWorkbenchAPI;

    public MedRecordTools() {
    }

    /**
     * 查询患者基本信息
     */
    @Tool(name = "queryPatientInfo", description = "根据患者ID查询患者基本信息（姓名、性别、年龄、联系方式等），返回 JSON，用于生成电子病历")
    public String queryPatientInfo(Long patientId) {
        log.info("[Tool] queryPatientInfo, patientId={}", patientId);

        if (patientId == null) {
            return "{\"error\":\"patientId is required\"}";
        }

        try {
            GetPatientInfoResponse response = userIdentityAPI.getPatientInfoByUserId(patientId);
            if (response == null || response.getPatient() == null) {
                return gson.toJson(new PatientInfoResult(null));
            }

            PatientVO patient = response.getPatient();
            PatientInfo info = new PatientInfo();
            info.setPatientId(patient.getUserId());
            info.setPatientName(patient.getUsername());
            info.setNickname(patient.getNickname());
            info.setGender(patient.getGender());
            info.setGenderDesc(patient.getGender() != null ? getGenderDesc(patient.getGender()) : "未知");
            info.setPhone(patient.getPhone());
            info.setBirthday(patient.getBirthday());
            info.setMedicalHistory(patient.getMedicalHistory());
            info.setAllergies(patient.getAllergies());
            info.setBloodType(patient.getBloodType());
            info.setEmergencyContact(patient.getEmergencyContact());
            info.setEmergencyPhone(patient.getEmergencyPhone());

            return gson.toJson(new PatientInfoResult(info));
        } catch (Exception e) {
            log.error("Failed to query patient info", e);
            return gson.toJson(new PatientInfoResult(null));
        }
    }

    /**
     * 查询患者历史健康报告
     */
    @Tool(name = "queryPatientHealthReports", description = "查询患者的历史健康报告列表，返回 JSON，用于了解患者既往健康状况")
    public String queryPatientHealthReports(Long patientId, Integer limit) {
        log.info("[Tool] queryPatientHealthReports, patientId={}, limit={}", patientId, limit);

        if (patientId == null) {
            return "{\"error\":\"patientId is required\"}";
        }

        if (limit == null || limit <= 0) {
            limit = 10;
        }

        try {
            QueryReportListRequest request = new QueryReportListRequest();
            request.setPatientId(patientId);
            request.setPageNum(1);
            request.setPageSize(limit);

            QueryReportListResponse response = healthReportAPI.queryReportList(request);
            if (response == null || response.getReportList() == null) {
                return gson.toJson(new HealthReportsResult(new ArrayList<>()));
            }

            List<HealthReportSummary> summaries = response.getReportList().stream()
                    .map(this::convertToReportSummary)
                    .collect(Collectors.toList());

            return gson.toJson(new HealthReportsResult(summaries));
        } catch (Exception e) {
            log.error("Failed to query patient health reports", e);
            return gson.toJson(new HealthReportsResult(new ArrayList<>()));
        }
    }

    /**
     * 查询患者历史诊疗日程
     */
    @Tool(name = "queryPatientScheduleHistory", description = "查询患者的历史诊疗日程，返回 JSON，用于了解患者既往诊疗过程")
    public String queryPatientScheduleHistory(Long patientId, Integer limit) {
        log.info("[Tool] queryPatientScheduleHistory, patientId={}, limit={}", patientId, limit);

        if (patientId == null) {
            return "{\"error\":\"patientId is required\"}";
        }

        if (limit == null || limit <= 0) {
            limit = 10;
        }

        try {
            QueryScheduleRequest request = new QueryScheduleRequest();
            request.setPageNum(1);
            request.setPageSize(limit);

            QueryScheduleResponse response = doctorWorkbenchAPI.querySchedule(request);
            if (response == null || response.getSchedules() == null) {
                return gson.toJson(new ScheduleHistoryResult(new ArrayList<>()));
            }

            PageUtils pageUtils = response.getSchedules();
            if (pageUtils.getList() == null) {
                return gson.toJson(new ScheduleHistoryResult(new ArrayList<>()));
            }

            // 过滤出该患者的日程
            List<ScheduleSummary> summaries = pageUtils.getList().stream()
                    .filter(item -> item instanceof ScheduleVO)
                    .map(item -> (ScheduleVO) item)
                    .filter(s -> patientId.equals(s.getPatientId()))
                    .limit(limit)
                    .map(this::convertToScheduleSummary)
                    .collect(Collectors.toList());

            return gson.toJson(new ScheduleHistoryResult(summaries));
        } catch (Exception e) {
            log.error("Failed to query patient schedule history", e);
            return gson.toJson(new ScheduleHistoryResult(new ArrayList<>()));
        }
    }

    /**
     * 将健康报告转换为摘要
     */
    private HealthReportSummary convertToReportSummary(HealthReportVO report) {
        HealthReportSummary summary = new HealthReportSummary();
        summary.setReportId(report.getReportId());
        summary.setTitle(report.getTitle());
        summary.setCategory(report.getCategory());
        summary.setReportDate(report.getReportDate());
        summary.setStatus(report.getStatus());
        summary.setStatusDesc(report.getStatusDesc());
        summary.setDescription(report.getDescription());
        return summary;
    }

    /**
     * 将日程转换为摘要
     */
    private ScheduleSummary convertToScheduleSummary(ScheduleVO schedule) {
        ScheduleSummary summary = new ScheduleSummary();
        summary.setScheduleId(schedule.getId());
        summary.setScheduleDay(schedule.getScheduleDay());
        summary.setScheduleContent(schedule.getSchedule());
        summary.setStatus(schedule.getStatus());
        summary.setStatusDesc(schedule.getStatusDesc());
        summary.setResult(schedule.getResult());
        summary.setDoctorId(schedule.getDoctorId());
        summary.setDoctorName(schedule.getDoctorName());
        summary.setPatientId(schedule.getPatientId());
        return summary;
    }

    /**
     * 获取性别描述
     */
    private String getGenderDesc(Integer gender) {
        if (gender == null) return "未知";
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 患者信息结果封装
     */
    @Data
    public static class PatientInfoResult {
        private PatientInfo patient;

        public PatientInfoResult(PatientInfo patient) {
            this.patient = patient;
        }
    }

    /**
     * 患者信息
     */
    @Data
    public static class PatientInfo {
        private Long patientId;
        private String patientName;
        private String nickname;
        private Integer gender;
        private String genderDesc;
        private String phone;
        private String birthday;
        private String medicalHistory;
        private String allergies;
        private String bloodType;
        private String emergencyContact;
        private String emergencyPhone;
    }

    /**
     * 健康报告列表结果
     */
    @Data
    public static class HealthReportsResult {
        private List<HealthReportSummary> reports;

        public HealthReportsResult(List<HealthReportSummary> reports) {
            this.reports = reports;
        }
    }

    /**
     * 健康报告摘要
     */
    @Data
    public static class HealthReportSummary {
        private Long reportId;
        private String title;
        private String category;
        private Long reportDate;
        private Integer status;
        private String statusDesc;
        private String description;
    }

    /**
     * 日程历史结果
     */
    @Data
    public static class ScheduleHistoryResult {
        private List<ScheduleSummary> schedules;

        public ScheduleHistoryResult(List<ScheduleSummary> schedules) {
            this.schedules = schedules;
        }
    }

    /**
     * 日程摘要
     */
    @Data
    public static class ScheduleSummary {
        private Long scheduleId;
        private String scheduleDay;
        private String scheduleContent;
        private String status;
        private String statusDesc;
        private String result;
        private Long doctorId;
        private String doctorName;
        private Long patientId;
    }
}

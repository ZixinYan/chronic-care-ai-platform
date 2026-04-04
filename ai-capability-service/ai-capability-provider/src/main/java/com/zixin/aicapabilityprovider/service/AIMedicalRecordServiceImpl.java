package com.zixin.aicapabilityprovider.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zixin.aicapabilityapi.api.AIMedicalRecordAPI;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordRequest;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordResponse;
import com.zixin.aicapabilityapi.vo.MedicalRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.zixin.aicapabilityprovider.tool.MedRecordTools;
import com.zixin.utils.exception.ToBCodeEnum;

/**
 * AI 电子病历生成服务实现
 *
 * 采用 Code-first ReAct 模式：
 * 1. 先查询患者信息、历史健康报告、历史诊疗日程等数据
 * 2. 将数据作为 Observation 注入 Prompt
 * 3. 调用 AI 生成结构化的电子病历
 */
@Slf4j
@Service
@DubboService
public class AIMedicalRecordServiceImpl implements AIMedicalRecordAPI {

    private final ChatClient chatClient;
    private final MedRecordTools medRecordTools;
    private final Gson gson = new GsonBuilder().create();

    public AIMedicalRecordServiceImpl(
            @Qualifier("medicalRecordChatClient") ChatClient chatClient,
            MedRecordTools medRecordTools) {
        this.chatClient = chatClient;
        this.medRecordTools = medRecordTools;
    }

    @Override
    public GenerateMedicalRecordResponse generateMedicalRecord(GenerateMedicalRecordRequest request) {
        GenerateMedicalRecordResponse response = new GenerateMedicalRecordResponse();

        // 1. 参数校验
        if (request == null || request.getScheduleId() == null) {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("日程ID不能为空");
            return response;
        }

        if (request.getPatientId() == null) {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("患者ID不能为空");
            return response;
        }

        try {
            // 2. 查询患者基本信息
            String patientInfoObservation = medRecordTools.queryPatientInfo(request.getPatientId());
            log.debug("Patient info observation: {}", patientInfoObservation);

            // 3. 查询患者历史健康报告
            String healthReportsObservation = medRecordTools.queryPatientHealthReports(request.getPatientId(), 10);
            log.debug("Health reports observation: {}", healthReportsObservation);

            // 4. 查询患者历史诊疗日程
            String scheduleHistoryObservation = medRecordTools.queryPatientScheduleHistory(request.getPatientId(), 5);
            log.debug("Schedule history observation: {}", scheduleHistoryObservation);

            // 5. 构建 Prompt
            String userPrompt = buildUserPrompt(request, patientInfoObservation, healthReportsObservation, scheduleHistoryObservation);

            // 6. 调用 AI 生成病历
            String modelResponse = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
            log.info("AI medical record response: {}", modelResponse);

            // 7. 解析 AI 响应
            MedicalRecordVO medicalRecord = parseMedicalRecord(request, modelResponse);

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("电子病历生成成功");
            response.setMedicalRecord(medicalRecord);

        } catch (Exception e) {
            log.error("Failed to generate medical record", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("电子病历生成失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 构建用户提示
     */
    private String buildUserPrompt(GenerateMedicalRecordRequest request,
                                    String patientInfoObservation,
                                    String healthReportsObservation,
                                    String scheduleHistoryObservation) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 电子病历生成任务\n\n");
        sb.append("请根据以下信息生成一份结构化的电子病历。\n\n");

        // 基本信息
        sb.append("## 本次诊疗信息\n\n");
        sb.append("- **就诊日期**: ").append(request.getScheduleDay()).append("\n");
        sb.append("- **就诊类型**: ").append(request.getScheduleCategoryName() != null ? request.getScheduleCategoryName() : "门诊").append("\n");
        sb.append("- **医生姓名**: ").append(request.getDoctorName() != null ? request.getDoctorName() : "未知").append("\n");
        sb.append("- **患者姓名**: ").append(request.getPatientName() != null ? request.getPatientName() : "未知").append("\n");

        if (request.getScheduleContent() != null && !request.getScheduleContent().isEmpty()) {
            sb.append("- **日程内容**: ").append(request.getScheduleContent()).append("\n");
        }

        if (request.getDiagnosisReport() != null && !request.getDiagnosisReport().isEmpty()) {
            sb.append("- **诊断报告**: ").append(request.getDiagnosisReport()).append("\n");
        }

        if (request.getPrescription() != null && !request.getPrescription().isEmpty()) {
            sb.append("- **处方信息**: ").append(request.getPrescription()).append("\n");
        }

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            sb.append("- **备注**: ").append(request.getNotes()).append("\n");
        }

        // 患者信息
        sb.append("\n## Observation · 患者基本信息\n\n");
        sb.append(patientInfoObservation).append("\n");

        // 历史健康报告
        sb.append("\n## Observation · 历史健康报告\n\n");
        sb.append(healthReportsObservation).append("\n");

        // 历史诊疗记录
        sb.append("\n## Observation · 历史诊疗记录\n\n");
        sb.append(scheduleHistoryObservation).append("\n");

        // 输出要求
        sb.append("\n---\n");
        sb.append("请根据以上信息生成电子病历，输出纯 JSON 格式（不要使用 Markdown 代码块）：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"chiefComplaint\": \"主诉（患者就诊的主要原因，一句话概括）\",\n");
        sb.append("  \"presentIllness\": \"现病史（本次发病过程、症状描述等）\",\n");
        sb.append("  \"pastHistory\": \"既往史（结合历史健康报告和历史诊疗记录分析）\",\n");
        sb.append("  \"diagnosis\": \"诊断意见（基于诊断报告提炼）\",\n");
        sb.append("  \"treatmentPlan\": \"治疗建议\",\n");
        sb.append("  \"prescription\": \"处方信息（如有）\",\n");
        sb.append("  \"precautions\": \"注意事项\",\n");
        sb.append("  \"followUpAdvice\": \"随访建议\"\n");
        sb.append("}\n");
        sb.append("```\n");

        return sb.toString();
    }

    /**
     * 解析 AI 响应为电子病历 VO
     */
    private MedicalRecordVO parseMedicalRecord(GenerateMedicalRecordRequest request, String modelResponse) {
        MedicalRecordVO vo = new MedicalRecordVO();

        // 设置基本信息
        vo.setScheduleId(request.getScheduleId());
        vo.setDoctorId(request.getDoctorId());
        vo.setDoctorName(request.getDoctorName());
        vo.setPatientId(request.getPatientId());
        vo.setPatientName(request.getPatientName());
        vo.setVisitDate(request.getScheduleDay());
        vo.setVisitType(request.getScheduleCategoryName());

        // 设置处方信息
        if (request.getPrescription() != null) {
            vo.setPrescription(request.getPrescription());
        }

        // 解析 JSON 响应
        String jsonStr = trimJsonFence(modelResponse);
        try {
            MedicalRecordContent content = gson.fromJson(jsonStr, MedicalRecordContent.class);
            if (content != null) {
                vo.setChiefComplaint(content.getChiefComplaint());
                vo.setPresentIllness(content.getPresentIllness());
                vo.setPastHistory(content.getPastHistory());
                vo.setDiagnosis(content.getDiagnosis());
                vo.setTreatmentPlan(content.getTreatmentPlan());
                if (vo.getPrescription() == null && content.getPrescription() != null) {
                    vo.setPrescription(content.getPrescription());
                }
                vo.setPrecautions(content.getPrecautions());
                vo.setFollowUpAdvice(content.getFollowUpAdvice());
            }
        } catch (Exception e) {
            log.warn("Failed to parse medical record JSON, using raw response", e);
            // 如果解析失败，将原始响应作为诊断意见
            vo.setDiagnosis(modelResponse);
        }

        // 生成完整病历内容
        vo.setFullContent(buildFullContent(vo, request));

        return vo;
    }

    /**
     * 构建完整病历内容（Markdown格式）
     */
    private String buildFullContent(MedicalRecordVO vo, GenerateMedicalRecordRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 电子病历\n\n");

        sb.append("## 基本信息\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("|------|------|\n");
        sb.append("| 就诊日期 | ").append(vo.getVisitDate() != null ? vo.getVisitDate() : "-").append(" |\n");
        sb.append("| 就诊类型 | ").append(vo.getVisitType() != null ? vo.getVisitType() : "-").append(" |\n");
        sb.append("| 患者 | ").append(vo.getPatientName() != null ? vo.getPatientName() : "-").append(" |\n");
        sb.append("| 医生 | ").append(vo.getDoctorName() != null ? vo.getDoctorName() : "-").append(" |\n\n");

        sb.append("## 主诉\n\n");
        sb.append(vo.getChiefComplaint() != null ? vo.getChiefComplaint() : "无").append("\n\n");

        sb.append("## 现病史\n\n");
        sb.append(vo.getPresentIllness() != null ? vo.getPresentIllness() : "无").append("\n\n");

        sb.append("## 既往史\n\n");
        sb.append(vo.getPastHistory() != null ? vo.getPastHistory() : "无").append("\n\n");

        sb.append("## 诊断意见\n\n");
        sb.append(vo.getDiagnosis() != null ? vo.getDiagnosis() : "无").append("\n\n");

        sb.append("## 治疗建议\n\n");
        sb.append(vo.getTreatmentPlan() != null ? vo.getTreatmentPlan() : "无").append("\n\n");

        if (vo.getPrescription() != null && !vo.getPrescription().isEmpty()) {
            sb.append("## 处方信息\n\n");
            sb.append(vo.getPrescription()).append("\n\n");
        }

        sb.append("## 注意事项\n\n");
        sb.append(vo.getPrecautions() != null ? vo.getPrecautions() : "无特殊注意事项").append("\n\n");

        sb.append("## 随访建议\n\n");
        sb.append(vo.getFollowUpAdvice() != null ? vo.getFollowUpAdvice() : "定期复查").append("\n\n");

        sb.append("---\n\n");
        sb.append("*本病历由 AI 辅助生成，仅供参考，请以医生实际诊断为准。*");

        return sb.toString();
    }

    /**
     * 去除 JSON 代码块标记
     */
    private String trimJsonFence(String raw) {
        if (raw == null) {
            return "{}";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            }
            int endFence = s.lastIndexOf("```");
            if (endFence >= 0) {
                s = s.substring(0, endFence);
            }
            return s.trim();
        }
        return s;
    }

    /**
     * 电子病历内容内部类
     */
    @lombok.Data
    private static class MedicalRecordContent {
        private String chiefComplaint;
        private String presentIllness;
        private String pastHistory;
        private String diagnosis;
        private String treatmentPlan;
        private String prescription;
        private String precautions;
        private String followUpAdvice;
    }
}

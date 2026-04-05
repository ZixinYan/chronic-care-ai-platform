package com.zixin.bloodglucoseconsumer.controller;

import com.zixin.accountapi.api.UserIdentityAPI;
import com.zixin.accountapi.dto.GetPatientInfoRequest;
import com.zixin.accountapi.dto.GetPatientInfoResponse;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.aicapabilityapi.api.AIGlucoseReportAPI;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportRequest;
import com.zixin.aicapabilityapi.dto.GenerateGlucoseReportResponse;
import com.zixin.bloodglucoseapi.api.GlucosePredictionAPI;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseRequest;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseResponse;
import com.zixin.healthcenterapi.api.HealthReportAPI;
import com.zixin.healthcenterapi.dto.SaveTextReportRequest;
import com.zixin.healthcenterapi.dto.SaveTextReportResponse;
import com.zixin.healthcenterapi.enums.ReportCategory;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.BusinessException;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.security.RequireRole;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 血糖预测控制器
 *
 * 提供血糖数据预测HTTP接口
 */
@Slf4j
@RestController
@RequestMapping("/glucose")
public class GlucosePredictionController {

    @DubboReference(check = false)
    private GlucosePredictionAPI glucosePredictionAPI;

    @DubboReference(check = false)
    private HealthReportAPI healthReportAPI;

    @DubboReference(check = false)
    private UserIdentityAPI userIdentityAPI;

    @DubboReference(check = false)
    private AIGlucoseReportAPI aiGlucoseReportAPI;

    /**
     * 预测未来血糖变化
     *
     * 功能说明:
     * 1. 接收多维度血糖数据
     * 2. 调用AI模型预测未来血糖
     * 3. 同步上传预测报告到数据库
     * 4. 返回预测结果
     *
     * 权限要求:
     * - 需要PATIENT角色
     *
     * @param httpRequest HTTP请求体，包含血糖预测所需参数
     * @return 预测结果
     */
    @PostMapping("/predict")
    @RequireRole("PATIENT")
    public Result<PredictGlucoseResponse> predictGlucose(
            @RequestBody PredictGlucoseRequest httpRequest) {

        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("predictGlucose - patientId: {}, cbgSize: {}, mealStatus: {}, predictHours: {}, traceId: {}",
                patientId, httpRequest.getCbg() != null ? httpRequest.getCbg().size() : 0, 
                httpRequest.getMealStatus(), httpRequest.getPredictHours(), traceId);

        PredictGlucoseRequest request = new PredictGlucoseRequest();
        request.setPatientId(patientId);
        request.setCbg(httpRequest.getCbg());
        request.setFinger(httpRequest.getFinger());
        request.setBasal(httpRequest.getBasal());
        request.setHr(httpRequest.getHr());
        request.setGsr(httpRequest.getGsr());
        request.setCarbInput(httpRequest.getCarbInput());
        request.setBolus(httpRequest.getBolus());
        request.setMealStatus(httpRequest.getMealStatus() != null ? httpRequest.getMealStatus() : 1);
        request.setPredictHours(httpRequest.getPredictHours() != null ? httpRequest.getPredictHours() : 3);

        PredictGlucoseResponse response = glucosePredictionAPI.predictGlucose(request);

        if (!ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            throw new BusinessException(response.getMessage());
        }

        uploadPredictionReport(patientId, httpRequest.getCbg(), response, httpRequest.getMealStatus(), httpRequest.getPredictHours(), traceId);

        return Result.success(response);
    }

    /**
     * 简化版血糖预测
     * 只接收CGM数据进行预测
     * @deprecated 建议使用 {@link #predictGlucose} 完整版接口，支持多维度数据输入
     */
    @Deprecated
    @PostMapping("/predict/simple")
    @RequireRole("PATIENT")
    public Result<PredictGlucoseResponse> predictGlucoseSimple(
            @RequestBody List<Double> cbg,
            @RequestParam(defaultValue = "1", value = "mealStatus") Integer mealStatus,
            @RequestParam(defaultValue = "3", value = "predictHours") Integer predictHours) {

        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("predictGlucoseSimple - patientId: {}, cbgSize: {}, traceId: {}",
                patientId, cbg != null ? cbg.size() : 0, traceId);

        PredictGlucoseRequest request = new PredictGlucoseRequest();
        request.setPatientId(patientId);
        request.setCbg(cbg);
        request.setMealStatus(mealStatus);
        request.setPredictHours(predictHours);

        PredictGlucoseResponse response = glucosePredictionAPI.predictGlucose(request);

        if (!ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            throw new BusinessException(response.getMessage());
        }

        uploadPredictionReport(patientId, cbg, response, mealStatus, predictHours, traceId);

        return Result.success(response);
    }

    /**
     * 保存血糖预测报告到数据库（轻量入库，不触发排班）
     * 保存失败时抛出异常，提示用户重试
     */
    private void uploadPredictionReport(Long patientId, List<Double> currentGlucoseValues,
                                        PredictGlucoseResponse prediction,
                                        Integer mealStatus, Integer predictHours, String traceId) {
        PatientVO patient = getPatientInfo(patientId);
        String patientName = patient != null ? patient.getNickname() : "患者";

        String mealLabel = mealStatus == 1 ? "空腹" : mealStatus == 2 ? "餐前" : "餐后";
        StringBuilder content = new StringBuilder();
        content.append("血糖预测报告\n");
        content.append("预测时长: ").append(predictHours).append(" 小时\n");
        content.append("用餐状态: ").append(mealLabel).append("\n");
        content.append("预测置信度: ").append(String.format("%.0f%%", prediction.getConfidence() * 100)).append("\n");
        content.append("预测血糖值(mg/dL): ").append(prediction.getPredictedValues()).append("\n");

        String healthSuggestions = generateAIHealthSuggestions(
                patientId, currentGlucoseValues, prediction, mealStatus);

        if (healthSuggestions != null && !healthSuggestions.isEmpty()) {
            content.append("\n--- AI健康建议 ---\n");
            content.append(healthSuggestions);
        }

        SaveTextReportRequest saveRequest = new SaveTextReportRequest();
        saveRequest.setUploaderId(patientId);
        saveRequest.setUploaderName(patientName);
        saveRequest.setPatientId(patientId);
        saveRequest.setCategory(ReportCategory.GLUCOSE_PREDICTION.getDescription());
        saveRequest.setTitle("血糖预测报告");
        saveRequest.setTextContent(content.toString());
        saveRequest.setReportDate(LocalDate.now().toString());

        log.info("uploadPredictionReport - patientId: {}, uploaderName: {}, traceId: {}", 
                patientId, patientName, traceId);

        SaveTextReportResponse saveResponse = healthReportAPI.saveTextReport(saveRequest);

        if (!ToBCodeEnum.SUCCESS.equals(saveResponse.getCode())) {
            log.error("uploadPredictionReport - 保存失败, patientId: {}, msg: {}, traceId: {}",
                    patientId, saveResponse.getMessage(), traceId);
            throw new BusinessException("报告保存失败，请重试");
        }

        log.info("uploadPredictionReport - 保存成功, reportId: {}, patientId: {}, traceId: {}",
                saveResponse.getReportId(), patientId, traceId);
    }

    private PatientVO getPatientInfo(Long patientId) {
        try {
            GetPatientInfoResponse response = userIdentityAPI.getPatientInfo(
                    GetPatientInfoRequest.builder().userId(patientId).build());
            if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
                return response.getPatient();
            }
            log.warn("getPatientInfo - 获取患者信息失败, patientId: {}, msg: {}", 
                    patientId, response.getMessage());
        } catch (Exception e) {
            log.error("getPatientInfo - 获取患者信息异常, patientId: {}", patientId, e);
        }
        return null;
    }

    private String generateAIHealthSuggestions(Long patientId, List<Double> currentGlucoseValues,
                                                PredictGlucoseResponse prediction, Integer mealStatus) {
        try {
            if (currentGlucoseValues == null || currentGlucoseValues.isEmpty()) {
                log.warn("generateAIHealthSuggestions - 当前血糖数据为空，跳过AI建议生成");
                return null;
            }

            List<Double> predictedValues = prediction.getPredictedValues();
            if (predictedValues == null || predictedValues.isEmpty()) {
                log.warn("generateAIHealthSuggestions - 预测血糖数据为空，跳过AI建议生成");
                return null;
            }

            double maxPredictedMmol = predictedValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0) * 0.0555;

            double avgCurrentMmol = currentGlucoseValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0) * 0.0555;

            GenerateGlucoseReportRequest aiRequest = new GenerateGlucoseReportRequest();
            aiRequest.setPatientId(patientId);
            aiRequest.setCurrentGlucoseValues(currentGlucoseValues);
            aiRequest.setPredictedGlucoseValues(predictedValues);
            aiRequest.setMealType(mealStatus != null ? mealStatus : 1);
            aiRequest.setMaxPredictedMmol(maxPredictedMmol);
            aiRequest.setAvgCurrentMmol(avgCurrentMmol);
            aiRequest.setConfidence(prediction.getConfidence());
            aiRequest.setAlertTriggered(false);

            GenerateGlucoseReportResponse aiResponse = aiGlucoseReportAPI.generateHealthReport(aiRequest);

            if (ToBCodeEnum.SUCCESS.equals(aiResponse.getCode())) {
                String suggestions = aiResponse.getHealthSuggestions();
                String fullReport = aiResponse.getFullReportContent();
                
                if (fullReport != null && !fullReport.isEmpty()) {
                    return fullReport;
                }
                return suggestions;
            }

            log.warn("generateAIHealthSuggestions - AI建议生成失败, msg: {}", aiResponse.getMessage());
        } catch (Exception e) {
            log.error("generateAIHealthSuggestions - AI建议生成异常, patientId: {}", patientId, e);
        }
        return null;
    }
}
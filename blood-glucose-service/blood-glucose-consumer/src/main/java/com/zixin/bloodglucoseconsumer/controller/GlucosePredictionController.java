package com.zixin.bloodglucoseconsumer.controller;

import com.zixin.bloodglucoseapi.api.GlucosePredictionAPI;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseRequest;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseResponse;
import com.zixin.healthcenterapi.api.HealthReportAPI;
import com.zixin.healthcenterapi.dto.SaveTextReportRequest;
import com.zixin.healthcenterapi.dto.SaveTextReportResponse;
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
     * @param cbg CGM数据列表
     * @param finger 指尖血数据列表
     * @param basal 基础率列表
     * @param hr 心率列表
     * @param gsr 皮肤电反应列表
     * @param carbInput 碳水化合物摄入列表
     * @param bolus 大剂量胰岛素列表
     * @param mealStatus 用餐状态
     * @param predictHours 预测时长（小时）
     * @return 预测结果
     */
    @PostMapping("/predict")
    @RequireRole("PATIENT")
    public Result<PredictGlucoseResponse> predictGlucose(
            @RequestParam List<Double> cbg,
            @RequestParam(required = false) List<Double> finger,
            @RequestParam(required = false) List<Double> basal,
            @RequestParam(required = false) List<Double> hr,
            @RequestParam(required = false) List<Double> gsr,
            @RequestParam(required = false) List<Double> carbInput,
            @RequestParam(required = false) List<Double> bolus,
            @RequestParam(defaultValue = "1") Integer mealStatus,
            @RequestParam(defaultValue = "3") Integer predictHours) {

        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("predictGlucose - patientId: {}, cbgSize: {}, mealStatus: {}, predictHours: {}, traceId: {}",
                patientId, cbg != null ? cbg.size() : 0, mealStatus, predictHours, traceId);

        PredictGlucoseRequest request = new PredictGlucoseRequest();
        request.setPatientId(patientId);
        request.setCbg(cbg);
        request.setFinger(finger);
        request.setBasal(basal);
        request.setHr(hr);
        request.setGsr(gsr);
        request.setCarbInput(carbInput);
        request.setBolus(bolus);
        request.setMealStatus(mealStatus);
        request.setPredictHours(predictHours);

        PredictGlucoseResponse response = glucosePredictionAPI.predictGlucose(request);

        if (!ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            throw new BusinessException(response.getMessage());
        }

        uploadPredictionReport(patientId, response, mealStatus, predictHours, traceId);

        return Result.success(response);
    }

    /**
     * 简化版血糖预测
     * 只接收CGM数据进行预测
     */
    @PostMapping("/predict/simple")
    @RequireRole("PATIENT")
    public Result<PredictGlucoseResponse> predictGlucoseSimple(
            @RequestBody List<Double> cbg,
            @RequestParam(defaultValue = "1") Integer mealStatus,
            @RequestParam(defaultValue = "3") Integer predictHours) {

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

        uploadPredictionReport(patientId, response, mealStatus, predictHours, traceId);

        return Result.success(response);
    }

    /**
     * 保存血糖预测报告到数据库（轻量入库，不触发排班）
     * 保存失败时抛出异常，提示用户重试
     */
    private void uploadPredictionReport(Long patientId, PredictGlucoseResponse prediction,
                                        Integer mealStatus, Integer predictHours, String traceId) {
        String mealLabel = mealStatus == 1 ? "空腹" : mealStatus == 2 ? "餐前" : "餐后";
        StringBuilder content = new StringBuilder();
        content.append("血糖预测报告\n");
        content.append("预测时长: ").append(predictHours).append(" 小时\n");
        content.append("用餐状态: ").append(mealLabel).append("\n");
        content.append("预测置信度: ").append(String.format("%.0f%%", prediction.getConfidence() * 100)).append("\n");
        content.append("预测血糖值(mg/dL): ").append(prediction.getPredictedValues()).append("\n");

        SaveTextReportRequest saveRequest = new SaveTextReportRequest();
        saveRequest.setUploaderId(patientId);
        saveRequest.setPatientId(patientId);
        saveRequest.setCategory("blood_glucose_prediction");
        saveRequest.setTitle("血糖预测报告");
        saveRequest.setTextContent(content.toString());
        saveRequest.setReportDate(LocalDate.now().toString());

        log.info("uploadPredictionReport - patientId: {}, traceId: {}", patientId, traceId);

        SaveTextReportResponse saveResponse = healthReportAPI.saveTextReport(saveRequest);

        if (!ToBCodeEnum.SUCCESS.equals(saveResponse.getCode())) {
            log.error("uploadPredictionReport - 保存失败, patientId: {}, msg: {}, traceId: {}",
                    patientId, saveResponse.getMessage(), traceId);
            throw new BusinessException("报告保存失败，请重试");
        }

        log.info("uploadPredictionReport - 保存成功, reportId: {}, patientId: {}, traceId: {}",
                saveResponse.getReportId(), patientId, traceId);
    }
}
package com.zixin.healthcenterconsumer.controller;

import com.zixin.bloodglucoseapi.api.GlucosePredictionAPI;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseRequest;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseResponse;
import com.zixin.healthcenterapi.api.HealthReportAPI;
import com.zixin.healthcenterapi.dto.*;
import com.zixin.healthcenterapi.vo.AISummaryVO;
import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.BusinessException;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.security.RequireRole;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 血糖报告流程控制器
 *
 * 整合血糖预测和AI报告生成功能
 */
@Slf4j
@RestController
@RequestMapping("/glucose-report")
public class GlucoseReportController {

    @DubboReference(check = false)
    private GlucosePredictionAPI glucosePredictionAPI;

    @DubboReference(check = false)
    private HealthReportAPI healthReportAPI;

    /**
     * 完整血糖预测流程
     *
     * 功能说明:
     * 1. 接收多维度血糖数据
     * 2. 调用Python预测接口获取未来血糖值
     * 3. 调用ai-capability生成AI报告
     * 4. 自动保存到患者报告库
     * 5. 如果血糖超阈值则发送预警短信
     *
     * @param cbg CGM数据
     * @param finger 指尖血数据
     * @param basal 基础率
     * @param hr 心率
     * @param gsr 皮肤电反应
     * @param carbInput 碳水化合物摄入
     * @param bolus 大剂量胰岛素
     * @param mealType 用餐类型
     * @return 报告生成结果
     */
    @PostMapping("/predict-and-generate")
    @RequireRole("PATIENT")
    public Result<GenerateAIReportResponse> predictAndGenerateReport(
            @RequestParam List<Double> cbg,
            @RequestParam(required = false) List<Double> finger,
            @RequestParam(required = false) List<Double> basal,
            @RequestParam(required = false) List<Double> hr,
            @RequestParam(required = false) List<Double> gsr,
            @RequestParam(required = false) List<Double> carbInput,
            @RequestParam(required = false) List<Double> bolus,
            @RequestParam(defaultValue = "1") Integer mealType) {

        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("predictAndGenerateReport - patientId: {}, cbgSize: {}, mealType: {}, traceId: {}",
                patientId, cbg != null ? cbg.size() : 0, mealType, traceId);

        // 1. 构建血糖预测请求
        PredictGlucoseRequest predictRequest = new PredictGlucoseRequest();
        predictRequest.setPatientId(patientId);
        predictRequest.setCbg(cbg);
        predictRequest.setFinger(finger);
        predictRequest.setBasal(basal);
        predictRequest.setHr(hr);
        predictRequest.setGsr(gsr);
        predictRequest.setCarbInput(carbInput);
        predictRequest.setBolus(bolus);
        predictRequest.setMealStatus(mealType);
        predictRequest.setPredictHours(3);

        // 2. 调用血糖预测
        PredictGlucoseResponse predictResponse = glucosePredictionAPI.predictGlucose(predictRequest);
        if (!ToBCodeEnum.SUCCESS.equals(predictResponse.getCode())) {
            throw new BusinessException("血糖预测失败: " + predictResponse.getMessage());
        }

        log.info("predictAndGenerateReport - 血糖预测完成, predictedCount: {}",
                predictResponse.getPredictedValues().size());

        // 3. 构建AI报告生成请求（包含当前血糖和预测数据）
        GenerateAIReportRequest generateRequest = new GenerateAIReportRequest();
        generateRequest.setPatientId(patientId);
        generateRequest.setCurrentGlucoseValues(cbg); // 当前CGM数据
        generateRequest.setCurrentGlucoseTimes(generateCurrentTimes(cbg.size())); // 当前时间点
        generateRequest.setPredictedGlucoseValues(predictResponse.getPredictedValues());
        generateRequest.setPredictedTimes(predictResponse.getPredictedTimes());
        generateRequest.setPredictStartTime(System.currentTimeMillis());
        generateRequest.setMealType(mealType);
        generateRequest.setConfidence(predictResponse.getConfidence());

        // 构建数据维度信息
        GlucoseDataDimensions dimensions = new GlucoseDataDimensions();
        dimensions.setCbg(cbg);
        dimensions.setFinger(finger);
        dimensions.setBasal(basal);
        dimensions.setHr(hr);
        dimensions.setGsr(gsr);
        dimensions.setCarbInput(carbInput);
        dimensions.setBolus(bolus);
        generateRequest.setDataDimensions(dimensions);

        // 4. 生成AI报告
        GenerateAIReportResponse generateResponse = healthReportAPI.generateAIReport(generateRequest);

        if (ToBCodeEnum.SUCCESS.equals(generateResponse.getCode())) {
            log.info("predictAndGenerateReport - 报告生成成功, reportId: {}, alertTriggered: {}",
                    generateResponse.getReportId(), generateResponse.getAlertTriggered());
            return Result.success(generateResponse);
        } else {
            throw new BusinessException(generateResponse.getMessage());
        }
    }

    /**
     * 检查血糖阈值并发送预警
     *
     * @param cbgValue CBG值 (mg/dL)
     * @param mealType 用餐类型
     * @return 检查结果
     */
    @PostMapping("/check-alert")
    @RequireRole("PATIENT")
    public Result<CheckGlucoseAlertResponse> checkGlucoseAlert(
            @RequestParam Double cbgValue,
            @RequestParam(defaultValue = "1") Integer mealType) {

        Long patientId = UserInfoManager.getUserIdOrThrow();
        String traceId = UserInfoManager.getTraceId();

        log.info("checkGlucoseAlert - patientId: {}, cbgValue: {}, mealType: {}, traceId: {}",
                patientId, cbgValue, mealType, traceId);

        CheckGlucoseAlertRequest request = new CheckGlucoseAlertRequest();
        request.setPatientId(patientId);
        request.setCbgValue(cbgValue);
        request.setMealType(mealType);

        CheckGlucoseAlertResponse response = healthReportAPI.checkGlucoseAlert(request);

        if (ToBCodeEnum.SUCCESS.equals(response.getCode())) {
            return Result.success(response);
        } else {
            throw new BusinessException(response.getMessage());
        }
    }

    /**
     * 生成当前血糖数据的时间点（向前推，每5分钟一个点）
     */
    private List<Long> generateCurrentTimes(int size) {
        List<Long> times = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long interval = 5 * 60 * 1000; // 5分钟

        for (int i = size - 1; i >= 0; i--) {
            times.add(currentTime - i * interval);
        }
        return times;
    }
}
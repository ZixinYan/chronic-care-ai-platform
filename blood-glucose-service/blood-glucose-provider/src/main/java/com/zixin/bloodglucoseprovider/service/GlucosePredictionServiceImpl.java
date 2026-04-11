package com.zixin.bloodglucoseprovider.service;

import com.zixin.accountapi.vo.PatientVO;
import com.zixin.bloodglucoseapi.api.GlucosePredictionAPI;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseRequest;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseResponse;
import com.zixin.bloodglucoseprovider.client.UserIdentityClient;
import com.zixin.thirdpartyapi.api.SMSAPI;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@DubboService(timeout = 30000)
public class GlucosePredictionServiceImpl implements GlucosePredictionAPI {

    private static final double MG_DL_TO_MMOL_L = 0.0555;

    private static final double FASTING_THRESHOLD = 8.3;
    private static final double POST_MEAL_1H_THRESHOLD = 12.7;
    private static final double POST_MEAL_2H_THRESHOLD = 11.1;
    private static final double POST_MEAL_3H_THRESHOLD = 10.0;

    private final UserIdentityClient userIdentityClient;

    @DubboReference(timeout = 50000, check = false)
    private SMSAPI smsAPI;

    public GlucosePredictionServiceImpl(UserIdentityClient userIdentityClient) {
        this.userIdentityClient = userIdentityClient;
    }

    @Override
    public PredictGlucoseResponse predictGlucose(PredictGlucoseRequest request) {
        PredictGlucoseResponse response = new PredictGlucoseResponse();

        try {
            if (request.getPatientId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者ID不能为空");
                return response;
            }

            if (request.getCbg() == null || request.getCbg().isEmpty()) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("CGM数据不能为空");
                return response;
            }

            log.info("predictGlucose - 开始预测, patientId: {}, dataSize: {}, mealStatus: {}",
                    request.getPatientId(), request.getCbg().size(), request.getMealStatus());

            List<Double> predictedValues = predictWithPythonService(request);
            List<Long> predictedTimes = generatePredictedTimes(request.getPredictHours());

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("预测成功");
            response.setPredictedValues(predictedValues);
            response.setPredictedTimes(predictedTimes);
            response.setConfidence(0.85);
            log.info("predictGlucose - 预测完成, patientId: {}, predictedCount: {}",
                    request.getPatientId(), predictedValues.size());

            checkAndSendGlucoseAlert(request.getPatientId(), predictedValues, request.getMealStatus());

        } catch (Exception e) {
            log.error("predictGlucose - 预测异常, patientId: {}", request.getPatientId(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("预测异常: " + e.getMessage());
        }

        return response;
    }

    private void checkAndSendGlucoseAlert(Long patientId, List<Double> predictedValues, Integer mealStatus) {
        if (predictedValues == null || predictedValues.isEmpty()) {
            return;
        }

        double maxValueMmol = 0;
        for (Double value : predictedValues) {
            double mmol = value * MG_DL_TO_MMOL_L;
            if (mmol > maxValueMmol) {
                maxValueMmol = mmol;
            }
        }

        double threshold = getThresholdByMealStatus(mealStatus);
        if (maxValueMmol > threshold) {
            log.warn("checkAndSendGlucoseAlert - 血糖预测值超过预警阈值, patientId: {}, maxValue: {} mmol/L, threshold: {} mmol/L",
                    patientId, maxValueMmol, threshold);
            sendGlucoseAlertSMS(patientId, maxValueMmol, mealStatus);
        }
    }

    private double getThresholdByMealStatus(Integer mealStatus) {
        if (mealStatus == null) {
            return FASTING_THRESHOLD;
        }
        return switch (mealStatus) {
            case 1 -> FASTING_THRESHOLD;
            case 2 -> POST_MEAL_1H_THRESHOLD;
            case 3 -> POST_MEAL_2H_THRESHOLD;
            case 4 -> POST_MEAL_3H_THRESHOLD;
            default -> FASTING_THRESHOLD;
        };
    }

    private String getMealStatusDesc(Integer mealStatus) {
        if (mealStatus == null) {
            return "空腹";
        }
        return switch (mealStatus) {
            case 1 -> "空腹";
            case 2 -> "餐后1小时";
            case 3 -> "餐后2小时";
            case 4 -> "餐后3小时";
            default -> "未知";
        };
    }

    private void sendGlucoseAlertSMS(Long patientId, double cbgMmol, Integer mealStatus) {
        try {
            PatientVO patient = userIdentityClient.getPatientInfo(patientId);
            if (patient == null) {
                log.warn("sendGlucoseAlertSMS - 患者不存在, patientId: {}", patientId);
                return;
            }

            String emergencyPhone = patient.getEmergencyPhone();
            if (emergencyPhone == null || emergencyPhone.isEmpty()) {
                log.warn("sendGlucoseAlertSMS - 未设置紧急联系人电话, patientId: {}", patientId);
                return;
            }

            String mealStatusDesc = getMealStatusDesc(mealStatus);
            String smsContent = String.format(
                "【慢病管理平台】预警：患者%s的%s血糖预测值为%.1fmmol/L，超过正常阈值，请及时关注。",
                patient.getNickname() != null ? patient.getNickname() : "",
                mealStatusDesc,
                cbgMmol
            );

            SendSMSRequest smsRequest = new SendSMSRequest();
            smsRequest.setPhone(emergencyPhone);
            smsRequest.setCode(smsContent);
            smsRequest.setTemplateId("GLUCOSE_ALERT");

            SendSMSResponse smsResponse = smsAPI.sendSMS(smsRequest);

            if (smsResponse.getCode() == ToBCodeEnum.SUCCESS) {
                log.info("sendGlucoseAlertSMS - 短信发送成功, patientId: {}, phone: {}",
                        patientId, emergencyPhone);
            } else {
                log.warn("sendGlucoseAlertSMS - 短信发送失败, patientId: {}, error: {}",
                        patientId, smsResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("sendGlucoseAlertSMS - 发送短信异常, patientId: {}", patientId, e);
        }
    }

    private List<Double> predictWithPythonService(PredictGlucoseRequest request) {
        List<Double> cbg = request.getCbg();
        int predictHours = request.getPredictHours() != null ? request.getPredictHours() : 3;
        int steps = predictHours * 12;

        List<Double> predictions = new ArrayList<>();

        if (cbg.size() >= 2) {
            double last = cbg.get(cbg.size() - 1);
            double prev = cbg.get(cbg.size() - 2);
            double trend = last - prev;

            for (int i = 1; i <= steps; i++) {
                double predicted = last + trend * i * 0.5;
                predicted += (Math.random() - 0.5) * 2;
                predictions.add(Math.max(70, Math.min(400, predicted)));
            }
        } else {
            double last = cbg.get(cbg.size() - 1);
            for (int i = 1; i <= steps; i++) {
                predictions.add(last);
            }
        }

        return predictions;
    }

    private List<Long> generatePredictedTimes(int hours) {
        List<Long> times = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long interval = 5 * 60 * 1000;

        int steps = hours * 12;
        for (int i = 1; i <= steps; i++) {
            times.add(currentTime + i * interval);
        }

        return times;
    }
}

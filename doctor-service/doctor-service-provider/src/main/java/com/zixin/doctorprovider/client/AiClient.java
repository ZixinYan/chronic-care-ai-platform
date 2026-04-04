package com.zixin.doctorprovider.client;

import com.zixin.aicapabilityapi.api.AIMedicalRecordAPI;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordRequest;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordResponse;
import com.zixin.aicapabilityapi.vo.MedicalRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * AI 能力客户端
 *
 * 调用 AI 服务生成电子病历等
 */
@Slf4j
@Service
public class AiClient {

    @DubboReference(timeout = 60000, check = false)
    private AIMedicalRecordAPI aiMedicalRecordAPI;

    /**
     * 生成电子病历
     *
     * @param request 生成请求
     * @return 生成的病历，失败返回 null
     */
    public MedicalRecordVO generateMedicalRecord(GenerateMedicalRecordRequest request) {
        log.info("调用 AI 生成电子病历, scheduleId={}, patientId={}", request.getScheduleId(), request.getPatientId());
        try {
            GenerateMedicalRecordResponse response = aiMedicalRecordAPI.generateMedicalRecord(request);
            if (response != null && response.getMedicalRecord() != null) {
                log.info("AI 电子病历生成成功, scheduleId={}", request.getScheduleId());
                return response.getMedicalRecord();
            } else {
                log.warn("AI 电子病历生成失败, scheduleId={}, message={}",
                        request.getScheduleId(),
                        response != null ? response.getMessage() : "response is null");
                return null;
            }
        } catch (Exception e) {
            log.error("调用 AI 生成电子病历异常, scheduleId={}", request.getScheduleId(), e);
            return null;
        }
    }
}

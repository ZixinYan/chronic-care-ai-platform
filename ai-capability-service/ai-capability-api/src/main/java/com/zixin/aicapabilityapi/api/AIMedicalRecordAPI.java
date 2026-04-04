package com.zixin.aicapabilityapi.api;

import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordRequest;
import com.zixin.aicapabilityapi.dto.GenerateMedicalRecordResponse;

/**
 * AI 电子病历生成 API
 *
 * 提供基于诊疗过程自动生成电子病历的能力
 */
public interface AIMedicalRecordAPI {

    /**
     * 生成 AI 电子病历
     *
     * 根据日程完成时的诊断信息、患者健康数据等自动生成电子病历
     *
     * @param request 生成请求
     * @return 生成结果（包含病历内容）
     */
    GenerateMedicalRecordResponse generateMedicalRecord(GenerateMedicalRecordRequest request);
}

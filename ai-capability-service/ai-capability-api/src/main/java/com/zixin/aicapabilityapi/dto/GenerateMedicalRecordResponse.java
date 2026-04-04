package com.zixin.aicapabilityapi.dto;

import com.zixin.aicapabilityapi.vo.MedicalRecordVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AI 电子病历生成响应
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GenerateMedicalRecordResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 生成的电子病历
     */
    private MedicalRecordVO medicalRecord;
}

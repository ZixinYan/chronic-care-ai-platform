package com.zixin.bloodglucoseapi.dto;

import com.zixin.utils.utils.BaseResponse;
import com.zixin.utils.utils.Result;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 血糖预测响应
 */
@Data
public class PredictGlucoseResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 预测的未来血糖值列表 (mg/dL)
     */
    private List<Double> predictedValues;
    
    /**
     * 预测的未来血糖值列表 (mmol/L)
     */
    private List<Double> predictedValuesMmol;
    
    /**
     * 预测时间点列表 (时间戳)
     */
    private List<Long> predictedTimes;
    
    /**
     * 预测置信度 (0-1)
     */
    private Double confidence;
}
package com.zixin.bloodglucoseapi.api;

import com.zixin.bloodglucoseapi.dto.PredictGlucoseRequest;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseResponse;

/**
 * 血糖预测 Dubbo API
 *
 * 提供血糖数据预测功能
 */
public interface GlucosePredictionAPI {

    /**
     * 预测未来血糖变化
     *
     * 功能说明:
     * 1. 接收多维度血糖数据
     * 2. 调用Python预测服务【TODO: 集成外部Python接口】
     * 3. 返回未来几小时的血糖预测值
     *
     * @param request 预测请求
     * @return 预测响应
     */
    PredictGlucoseResponse predictGlucose(PredictGlucoseRequest request);
}
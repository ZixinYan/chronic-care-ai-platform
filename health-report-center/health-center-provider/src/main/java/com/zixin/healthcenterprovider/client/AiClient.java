package com.zixin.healthcenterprovider.client;

import com.zixin.aicapabilityapi.api.AIScheduleAPI;
import com.zixin.aicapabilityapi.dto.GenerateScheduleRequest;
import com.zixin.aicapabilityapi.dto.GenerateScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiClient {

    @DubboReference(timeout = 50000, check = false)
    private AIScheduleAPI aiScheduleAPI;

    public GenerateScheduleResponse generateSchedule(GenerateScheduleRequest request) {
        log.debug("调用 AI 生成排班建议, 请求参数: {}", request);
        try {
            GenerateScheduleResponse response = aiScheduleAPI.SmartScheduleGenerate(request);
            log.debug("AI 生成排班建议响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("调用 AI 生成排班建议失败", e);
            GenerateScheduleResponse errorResponse = new GenerateScheduleResponse();
            errorResponse.setCode(com.zixin.utils.exception.ToBCodeEnum.FAIL);
            errorResponse.setMessage("调用 AI 生成排班建议失败: " + e.getMessage());
            return errorResponse;
        }
    }
}

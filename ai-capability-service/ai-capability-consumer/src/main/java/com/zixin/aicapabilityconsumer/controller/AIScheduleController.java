package com.zixin.aicapabilityconsumer.controller;

import com.zixin.aicapabilityapi.api.AIScheduleAPI;
import com.zixin.aicapabilityapi.dto.GenerateScheduleRequest;
import com.zixin.aicapabilityapi.dto.GenerateScheduleResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/schedule")
public class AIScheduleController {

    @DubboReference(check = false)
    private AIScheduleAPI aischeduleAPI;

    @PostMapping("/generate")
    public Result<?> generateSchedule(@RequestBody GenerateScheduleRequest request) {
        GenerateScheduleResponse response = aischeduleAPI.SmartScheduleGenerate(request);
        
        if (response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            return Result.success(response);
        } else {
            return Result.error(response.getMessage());
        }
    }
}

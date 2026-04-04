package com.zixin.aicapabilityapi.api;

import com.zixin.aicapabilityapi.dto.GenerateScheduleRequest;
import com.zixin.aicapabilityapi.dto.GenerateScheduleResponse;
import com.zixin.aicapabilityapi.vo.SuggestScheduleVO;

import java.util.List;

public interface AIScheduleAPI {
    // AI生成医生日程建议
    GenerateScheduleResponse SmartScheduleGenerate(GenerateScheduleRequest request);
}

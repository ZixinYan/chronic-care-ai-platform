package com.zixin.thirdpartyapi.api;

import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.utils.utils.Result;

public interface SMSAPI {
    SendSMSResponse sendSMS(SendSMSRequest sendSMSRequest);
}

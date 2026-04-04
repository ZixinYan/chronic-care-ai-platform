package com.zixin.authconsumer.client;

import com.zixin.thirdpartyapi.api.OSSAPI;
import com.zixin.thirdpartyapi.api.SMSAPI;
import com.zixin.thirdpartyapi.dto.OSSUploadFileRequest;
import com.zixin.thirdpartyapi.dto.OSSUploadFileResponse;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ThirdPartyClient {
    @DubboReference(timeout = 2000000)
    private SMSAPI smsServiceImpl;
    @DubboReference(timeout = 2000000)
    private OSSAPI ossServiceImpl;

    public boolean sendSMS(SendSMSRequest sendSMSRequest) {
        SendSMSResponse response = smsServiceImpl.sendSMS(sendSMSRequest);
        if(response.getCode().equals(ToBCodeEnum.FAIL)){
            log.error("Failed to send SMS: {}", response.getMessage());
            return false;
        }
        return true;
    }

    public String uploadFile(OSSUploadFileRequest request) {
        OSSUploadFileResponse response = ossServiceImpl.uploadFile(request);
        if(response.getCode().equals(ToBCodeEnum.FAIL)){
            log.error("Failed to upload file: {}", response.getMessage());
            return "";
        }
        return response.getUrl();
    }
}

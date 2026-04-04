package com.zixin.messageprovider.client;

import com.zixin.messageprovider.runner.SMSProducer;
import com.zixin.thirdpartyapi.api.SMSAPI;
import com.zixin.thirdpartyapi.dto.SendEmailRequest;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SMSClient {

    @DubboReference(timeout = 200000)
    private SMSAPI thirdPartySMSAPI;

    private final SMSProducer smsProducer;

    public SMSClient(SMSProducer smsProducer) {
        this.smsProducer = smsProducer;
    }

    public boolean sendSMS(SendSMSRequest request) {
        SendSMSResponse smsResponse = thirdPartySMSAPI.sendSMS(request);
        if (!ToBCodeEnum.SUCCESS.equals(smsResponse.getCode())) {
            log.error("Send SMS failed, receiverId: {}, error: {}",
                    request.getPhone(), smsResponse.getMessage());
            return false;
        }
        return true;
    }

    public boolean sendSMSByKafka(SendSMSRequest request) {
        try {
            smsProducer.sendSMSTask(request);
            return false;
        } catch (Exception e) {
            log.error("Send email task failed, receiverId: {}", request.getPhone(), e);
            return true;
        }
    }

}

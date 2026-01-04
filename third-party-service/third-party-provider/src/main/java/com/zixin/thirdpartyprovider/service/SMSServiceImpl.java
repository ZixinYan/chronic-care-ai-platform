package com.zixin.thirdpartyprovider.service;

import com.zixin.thirdpartyapi.api.SMSAPI;
import com.zixin.thirdpartyapi.dto.SendSMSRequest;
import com.zixin.thirdpartyapi.dto.SendSMSResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.HttpUtils;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@DubboService
@Slf4j
public class SMSServiceImpl implements SMSAPI {
    @Value("${spring.cloud.alicloud.sms.host}")
    private String host;
    @Value("${spring.cloud.alicloud.sms.path}")
    private String path;
    @Value("${spring.cloud.alicloud.sms.app_code}")
    private String appCode;
    @Value("${spring.cloud.alicloud.sms.tpl_id}")
    private String tpl_id;

    @Override
    public SendSMSResponse sendSMS(SendSMSRequest sendSMSRequest) {
        SendSMSResponse sendSMSResponse = new SendSMSResponse();

        // String host = "https://send.market.alicloudapi.com";
        // String path = "/sms/send";
        String method = "POST";
        // String appcode = "45784e7cf368430caa9236e042d41bf2";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appCode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:"+ sendSMSRequest.getCode());
        bodys.put("templateid", tpl_id);
        bodys.put("mobile", sendSMSRequest.getPhone());


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            log.info(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            log.error("fail to send sms request:{}", e.getMessage());
            sendSMSResponse.setCode(ToBCodeEnum.FAIL);
            sendSMSResponse.setMessage("fail to send sms request");
            return sendSMSResponse;
        }
        sendSMSResponse.setCode(ToBCodeEnum.SUCCESS);
        return sendSMSResponse;
    }
}

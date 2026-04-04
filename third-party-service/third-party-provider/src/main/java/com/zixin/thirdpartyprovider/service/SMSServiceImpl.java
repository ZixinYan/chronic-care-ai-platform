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
@Service
public class SMSServiceImpl implements SMSAPI {
    @Value("${spring.cloud.alicloud.sms.host}")
    private String host;
    @Value("${spring.cloud.alicloud.sms.path}")
    private String path;
    @Value("${spring.cloud.alicloud.sms.app_code}")
    private String appCode;
    @Override
    public SendSMSResponse sendSMS(SendSMSRequest sendSMSRequest) {
        SendSMSResponse sendSMSResponse = new SendSMSResponse();
        String method = "POST";
        // 根据阿里云短信服务的API文档，构建请求头和查询参数
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "APPCODE " + appCode);
        // 根据阿里云短信服务的API文档，构建查询参数
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("content", "【健康智能小助手】杂鱼大哥哥，你的验证码是：" + sendSMSRequest.getCode()+ "，5分钟内有效！");
        querys.put("mobile", sendSMSRequest.getPhone());
        Map<String, String> bodys = new HashMap<String, String>();
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

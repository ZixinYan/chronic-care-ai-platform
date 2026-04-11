package com.zixin.bloodglucoseprovider.client;

import com.zixin.accountapi.api.UserIdentityAPI;
import com.zixin.accountapi.dto.GetPatientInfoRequest;
import com.zixin.accountapi.dto.GetPatientInfoResponse;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserIdentityClient {

    @DubboReference(timeout = 50000, check = false)
    private UserIdentityAPI userIdentityAPI;

    public PatientVO getPatientInfo(Long userId) {
        GetPatientInfoResponse response = userIdentityAPI.getPatientInfo(GetPatientInfoRequest.builder()
                .userId(userId)
                .build());

        if (!response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            log.error("Failed to get patient info for userId: {}, message: {}", userId, response.getMessage());
            return null;
        }

        return response.getPatient();
    }
}

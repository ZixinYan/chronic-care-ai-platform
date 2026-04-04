package com.zixin.healthcenterconsumer.client;

import com.zixin.thirdpartyapi.api.OSSAPI;
import com.zixin.thirdpartyapi.dto.OSSUploadFileRequest;
import com.zixin.thirdpartyapi.dto.OSSUploadFileResponse;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class OSSClient {
    @DubboReference(check = false,timeout = 2000000)
    private OSSAPI ossAPI;

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.error("File is null or empty");
            return null;
        }
        
        OSSUploadFileRequest request = new OSSUploadFileRequest();
        request.setFile(file.getInputStream().readAllBytes());
        request.setFileName(file.getOriginalFilename());
        
        OSSUploadFileResponse response = ossAPI.uploadFile(request);
        if (response.getCode().equals(ToBCodeEnum.FAIL)) {
            log.error("OSS upload failed: {}", response.getMessage());
            return null;
        }
        
        return response.getUrl();
    }
}


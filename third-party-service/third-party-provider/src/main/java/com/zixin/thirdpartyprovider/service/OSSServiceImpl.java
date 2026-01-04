package com.zixin.thirdpartyprovider.service;

import com.zixin.thirdpartyapi.api.OSSAPI;
import com.zixin.thirdpartyapi.dto.OSSMultiUploadFileRequest;
import com.zixin.thirdpartyapi.dto.OSSMultiUploadFileResponse;
import com.zixin.thirdpartyapi.dto.OSSUploadFileRequest;
import com.zixin.thirdpartyapi.dto.OSSUploadFileResponse;
import com.zixin.thirdpartyprovider.utils.AliOSSUtils;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@DubboService
public class OSSServiceImpl implements OSSAPI {
    @Override
    public OSSUploadFileResponse uploadFile(OSSUploadFileRequest ossUploadFileRequest) {
        String name = ossUploadFileRequest.getFile().getOriginalFilename();
        OSSUploadFileResponse ossUploadFileResponse = new OSSUploadFileResponse();
        String filename = null;
        if (name == null) {
            log.error("file name is null");
            ossUploadFileResponse.setCode(ToBCodeEnum.FAIL);
            ossUploadFileResponse.setMessage("file name is null");
            return ossUploadFileResponse;
        }
        filename = UUID.randomUUID().toString()+name.substring(name.lastIndexOf("."));
        String url = null;
        try {
            url = AliOSSUtils.uploadFile(filename, ossUploadFileRequest.getFile().getInputStream());
        } catch (IOException e) {
            log.error("upload file error:{}, reason:{}",filename,e.getMessage());
            ossUploadFileResponse.setCode(ToBCodeEnum.FAIL);
            ossUploadFileResponse.setMessage("file name is null");
            return ossUploadFileResponse;
        }
        ossUploadFileResponse.setCode(ToBCodeEnum.SUCCESS);
        ossUploadFileResponse.setUrl(url);
        return ossUploadFileResponse;
    }

    @Override
    public OSSMultiUploadFileResponse multiUpload(OSSMultiUploadFileRequest ossMultiUploadFileRequest) {
        String name = ossMultiUploadFileRequest.getFile().getOriginalFilename();
        OSSMultiUploadFileResponse ossMultiUploadFileResponse = new OSSMultiUploadFileResponse();
        String filename;
        if (name == null) {
            log.error("file name is null");
            ossMultiUploadFileResponse.setCode(ToBCodeEnum.FAIL);
            ossMultiUploadFileResponse.setMessage("file name is null");
            return ossMultiUploadFileResponse;
        }
        filename = UUID.randomUUID().toString()+name.substring(name.lastIndexOf("."));
        String url = null;
        try {
            url = AliOSSUtils.multipartUploadByStream(filename, ossMultiUploadFileRequest.getFile().getInputStream(),
                    ossMultiUploadFileRequest.getPartSize(), ossMultiUploadFileRequest.getMaxRetry());
        } catch (Exception e) {
            log.error("multi upload error:{}, reason:{}",filename,e.getMessage());
            ossMultiUploadFileResponse.setCode(ToBCodeEnum.FAIL);
            ossMultiUploadFileResponse.setMessage("multi upload error");
            return ossMultiUploadFileResponse;
        }
        ossMultiUploadFileResponse.setCode(ToBCodeEnum.SUCCESS);
        ossMultiUploadFileResponse.setUrl(url);
        return ossMultiUploadFileResponse;
    }

}

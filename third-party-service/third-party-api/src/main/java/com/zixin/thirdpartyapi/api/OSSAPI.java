package com.zixin.thirdpartyapi.api;

import com.zixin.thirdpartyapi.dto.OSSMultiUploadFileRequest;
import com.zixin.thirdpartyapi.dto.OSSMultiUploadFileResponse;
import com.zixin.thirdpartyapi.dto.OSSUploadFileRequest;
import com.zixin.thirdpartyapi.dto.OSSUploadFileResponse;
import com.zixin.utils.utils.Result;
import org.springframework.web.multipart.MultipartFile;

public interface OSSAPI {
    OSSUploadFileResponse uploadFile(OSSUploadFileRequest ossUploadFileRequest);
    OSSMultiUploadFileResponse multiUpload(OSSMultiUploadFileRequest ossMultiUploadFileRequest);
}

package com.zixin.thirdpartyapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.multipart.MultipartFile;

@EqualsAndHashCode(callSuper = true)
@Data
public class OSSMultiUploadFileRequest extends OSSUploadFileRequest {
    private long partSize;
    private int maxRetry;
}

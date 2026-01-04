package com.zixin.thirdpartyapi.dto;

import com.zixin.utils.utils.BaseResponse;
import lombok.Data;

@Data
public class OSSUploadFileResponse extends BaseResponse {
    private String url;
}

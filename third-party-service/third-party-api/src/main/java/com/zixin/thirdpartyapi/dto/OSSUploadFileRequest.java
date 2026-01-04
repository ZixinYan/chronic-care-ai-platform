package com.zixin.thirdpartyapi.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Data
public class OSSUploadFileRequest {
    private String objectName;
    private MultipartFile file;
}

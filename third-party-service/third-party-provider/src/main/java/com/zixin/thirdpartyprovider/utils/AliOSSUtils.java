package com.zixin.thirdpartyprovider.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import com.zixin.thirdpartyapi.dto.OSSUploadFileRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 没有提供下载和删除接口，如有需要自己扩展
 */
@Slf4j
public class AliOSSUtils {
    @Value("${spring.oss.endpoint}")
    private static String ENDPOINT;

    @Value("${spring.oss.access_key_id}")
    private static String ACCESS_KEY_ID;

    @Value("${spring.oss.access_key_secret}")
    private static String ACCESS_KEY_SECRET;

    @Value("${spring.oss.bucket_name}")
    private static String BUCKET_NAME;


    public static String uploadFile(String objectName, InputStream in) throws ClientException {
        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用 V4 签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET, clientBuilderConfiguration);
        // 上传url
        String url = "";
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);
            // 上传字符串。
            putObjectWithRetry(ossClient, putObjectRequest, 3);
            url = generatePresignedUrl(ossClient,objectName);
        } catch (OSSException oe) {
            log.error("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            log.error("Error Message,{}",  oe.getErrorMessage());
            log.error("Error Code,{}" , oe.getErrorCode());
            log.error("Request ID,{}" , oe.getRequestId());
            log.error("Host ID,{}" , oe.getHostId());
        } catch (ClientException ce) {
            log.error("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            log.error("Error Message: {}" , ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }

    public static String multipartUploadByStream(String objectName, InputStream inputStream, long partSize, int maxRetry) throws Exception {

        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V4);

        OSS ossClient = new OSSClientBuilder()
                .build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET, conf);

        String uploadId = null;
        List<PartETag> partETags = new ArrayList<>();

        try {
            // 1. 初始化分片上传
            InitiateMultipartUploadRequest initReq =
                    new InitiateMultipartUploadRequest(BUCKET_NAME, objectName);
            uploadId = ossClient.initiateMultipartUpload(initReq).getUploadId();

            byte[] buffer = new byte[(int) partSize];
            int bytesRead;
            int partNumber = 1;

            // 2. 顺序读取 InputStream 并上传
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                ByteArrayInputStream partStream =
                        new ByteArrayInputStream(buffer, 0, bytesRead);

                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(BUCKET_NAME);
                uploadPartRequest.setKey(objectName);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setPartNumber(partNumber);
                uploadPartRequest.setInputStream(partStream);
                uploadPartRequest.setPartSize(bytesRead);

                UploadPartResult result =
                        uploadPartWithRetry(ossClient, uploadPartRequest, maxRetry);
                partETags.add(result.getPartETag());

                partETags.add(result.getPartETag());

                partNumber++;
            }

            // 3. 完成分片上传
            CompleteMultipartUploadRequest completeRequest =
                    new CompleteMultipartUploadRequest(
                            BUCKET_NAME, objectName, uploadId, partETags);

            ossClient.completeMultipartUpload(completeRequest);
            return generatePresignedUrl(ossClient,objectName);

        } catch (Exception e) {
            // 上传失败中止
            if (uploadId != null) {
                log.error("upload error:{}, reason:{}",uploadId,e.getMessage());
                ossClient.abortMultipartUpload(new AbortMultipartUploadRequest(BUCKET_NAME, objectName, uploadId));
                if(!abortMultipartUpload(objectName, uploadId)){
                    log.error("upload cancel error:{}",uploadId);
                }
                log.info("multipart upload cancel success:{}",uploadId);
            }
            return "";
        } finally {
            inputStream.close();
            ossClient.shutdown();
        }
    }

    // 清楚碎片文件
    public static boolean abortMultipartUpload(String objectName, String uploadId) throws Exception {
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V4);

        OSS ossClient = new OSSClientBuilder()
                .build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET, conf);
        try {
            // 取消分片上传
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(BUCKET_NAME, objectName, uploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);

            log.info("cancel success，upload ID: {}", uploadId);

        } catch (Exception e) {
            log.error("cancel upload failed:{}", e.getMessage());
            return false;
        } finally {
            // 关闭客户端连接
            ossClient.shutdown();
        }
        return true;
    }


    private static String generatePresignedUrl(OSS ossClient, String objectName) {
        // URL 有效期：1 小时
        Date expiration = new Date(System.currentTimeMillis() + 3600_000);
        URL url = ossClient.generatePresignedUrl(
                BUCKET_NAME, objectName, expiration);
        return url.toString();
    }


    private static UploadPartResult uploadPartWithRetry(
            OSS ossClient,
            UploadPartRequest request,
            int maxRetry
    ) {
        int attempt = 0;
        while (true) {
            try {
                return ossClient.uploadPart(request);
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetry) {
                    throw e;
                }
                try {
                    Thread.sleep(300L * attempt);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static void putObjectWithRetry(
            OSS ossClient,
            PutObjectRequest request,
            int maxRetry
    ) {
        int attempt = 0;
        while (true) {
            try {
                ossClient.putObject(request);
                return;
            } catch (ClientException | OSSException e) {
                attempt++;
                if (attempt >= maxRetry) {
                    log.error("upload file failed,{}",e.getMessage());
                    return;
                }
                try {
                    Thread.sleep(300L * attempt);
                } catch (InterruptedException ignored) {}
            }
        }
    }


}
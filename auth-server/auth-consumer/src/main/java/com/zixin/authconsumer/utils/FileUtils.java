package com.zixin.authconsumer.utils;

import com.zixin.utils.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public final class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    /**
     * 验证图片文件
     */
    public static Result validateImageFile(MultipartFile file) {
        // 检查文件是否存在
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 检查文件名
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return Result.error("文件名无效");
        }

        // 检查文件大小（5MB）
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("图片大小不能超过5MB，当前大小为：" + (file.getSize() / 1024 / 1024) + "MB");
        }

        // 检查文件类型（通过contentType）
        if (contentType == null || !isAllowedImageType(contentType)) {
            return Result.error("只支持 PNG、JPG 或 JPEG 格式的图片，当前类型为：" + contentType);
        }

        // 检查文件扩展名
        if (!hasAllowedExtension(originalFilename)) {
            return Result.error("文件扩展名必须是 .png、.jpg 或 .jpeg");
        }

        // 可选：通过文件内容验证
        try {
            if (!isValidImageByContent(file.getBytes())) {
                return Result.error("文件内容不是有效的图片格式");
            }
        } catch (IOException e) {
            log.error("Failed to read file content for validation", e);
            return Result.error("文件读取失败");
        }

        return null; // 验证通过
    }

    /**
     * 判断是否为允许的图片类型
     */
    private static boolean isAllowedImageType(String contentType) {
        return contentType.equals("image/png") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/jpeg") ||
                contentType.equals("image/pjpeg") ||  // 兼容
                contentType.equals("image/x-png");    // 兼容
    }

    /**
     * 判断文件是否有允许的扩展名
     */
    private static boolean hasAllowedExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return false;
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("png") ||
                extension.equals("jpg") ||
                extension.equals("jpeg") ||
                extension.equals("jpe") ||  // JPEG的另一种扩展名
                extension.equals("jfif");   // JPEG的另一种扩展名
    }

    /**
     * 通过文件内容判断类型（更安全的验证方式）
     */
    private static boolean isValidImageByContent(byte[] fileContent) {
        if (fileContent == null || fileContent.length < 8) {
            return false;
        }

        // 检查文件魔术数字
        // PNG: 前8个字节为 -119 P N G
        if (fileContent[0] == (byte) 0x89 &&
                fileContent[1] == (byte) 0x50 &&
                fileContent[2] == (byte) 0x4E &&
                fileContent[3] == (byte) 0x47) {
            return true;
        }

        // JPEG: 前2个字节为 -1 -32 (FF D8)
        if (fileContent[0] == (byte) 0xFF &&
                fileContent[1] == (byte) 0xD8) {
            return true;
        }

        return false;
    }
}

package com.community.controller;

import com.community.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_SUFFIXES = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    @Value("${upload.path:uploads/}")
    private String uploadPath;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
                return Result.error("文件格式不正确");
            }

            String suffix = originalFilename.substring(dotIndex).toLowerCase();
            if (!ALLOWED_SUFFIXES.contains(suffix)) {
                return Result.error("只支持 jpg、jpeg、png 格式");
            }

            if (file.getSize() > MAX_IMAGE_SIZE) {
                return Result.error("文件大小不能超过 5MB");
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                return Result.error("文件内容类型不正确");
            }

            byte[] bytes = file.getBytes();
            if (!isAllowedImage(bytes, suffix)) {
                return Result.error("文件内容不是有效图片");
            }

            String fileName = UUID.randomUUID().toString() + suffix;

            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path imageDir = baseDir.resolve("images").normalize();
            if (!imageDir.startsWith(baseDir)) {
                return Result.error("上传路径配置不合法");
            }
            Files.createDirectories(imageDir);

            Path dest = imageDir.resolve(fileName).normalize();
            if (!dest.startsWith(imageDir)) {
                return Result.error("上传文件路径不合法");
            }
            Files.write(dest, bytes);

            log.info("图片上传成功: {}, 文件大小: {} bytes", dest, bytes.length);

            String url = "/uploads/images/" + fileName;
            return Result.success(url);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    private boolean isAllowedImage(byte[] bytes, String suffix) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }

        boolean isJpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        boolean isPng = (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;

        if (".png".equals(suffix)) {
            return isPng;
        }
        return (".jpg".equals(suffix) || ".jpeg".equals(suffix)) && isJpeg;
    }
}

package com.campus.love.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 统一文件上传服务，供 Feed、Moment 等模块复用。
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${app.upload.path:./uploads/}")
    private String uploadPath;

    /** 图片 MIME 前缀 */
    public static final String MIME_IMAGE_PREFIX = "image/";
    /** 视频 MIME 前缀 */
    public static final String MIME_VIDEO_PREFIX = "video/";

    /** 图片扩展名白名单 */
    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg"};
    /** 视频扩展名白名单 */
    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".mov", ".webm", ".avi", ".mkv"};

    /**
     * 上传媒体文件（图片或视频）
     *
     * @param file        上传的文件
     * @param mediaType    IMAGE 或 VIDEO
     * @param maxSizeBytes 最大字节数
     * @param filenamePrefix 文件名前缀，如 feed_img_、feed_video_、moment_photo_
     * @param defaultExt   默认扩展名（当无法推断时使用），如 .jpg、.mp4
     * @return 访问路径，如 /uploads/xxx.jpg
     */
    public String uploadMedia(MultipartFile file, MediaType mediaType, long maxSizeBytes,
                             String filenamePrefix, String defaultExt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择文件");
        }
        String contentType = file.getContentType();
        String prefix = mediaType == MediaType.IMAGE ? MIME_IMAGE_PREFIX : MIME_VIDEO_PREFIX;
        if (contentType == null || !contentType.startsWith(prefix)) {
            String hint = mediaType == MediaType.IMAGE ? "仅支持图片格式" : "仅支持视频格式";
            throw new IllegalArgumentException(hint);
        }
        if (file.getSize() > maxSizeBytes) {
            String hint = mediaType == MediaType.IMAGE
                    ? "图片大小不能超过10MB"
                    : "视频大小不能超过100MB";
            throw new IllegalArgumentException(hint);
        }

        String ext = getFileExtension(file.getOriginalFilename(), contentType, mediaType);
        if (ext.isEmpty()) {
            ext = defaultExt;
        }
        String filename = filenamePrefix + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        File dir = getUploadDir();
        File dest = new File(dir, filename);
        file.transferTo(dest);

        return "/uploads/" + filename;
    }

    /** 上传图片，限制 10MB */
    public String uploadImage(MultipartFile file, String filenamePrefix) throws IOException {
        return uploadMedia(file, MediaType.IMAGE, 10L * 1024 * 1024, filenamePrefix, ".jpg");
    }

    /** 上传视频，限制 100MB */
    public String uploadVideo(MultipartFile file, String filenamePrefix) throws IOException {
        return uploadMedia(file, MediaType.VIDEO, 100L * 1024 * 1024, filenamePrefix, ".mp4");
    }

    public File getUploadDir() {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadPath);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private String getFileExtension(String filename, String contentType, MediaType mediaType) {
        if (filename != null && filename.contains(".")) {
            String lower = filename.toLowerCase();
            int lastDot = lower.lastIndexOf('.');
            String ext = lower.substring(lastDot);
            String[] allowed = mediaType == MediaType.IMAGE ? IMAGE_EXTENSIONS : VIDEO_EXTENSIONS;
            for (String a : allowed) {
                if (ext.equals(a)) return ext;
            }
        }
        if (contentType != null) {
            return switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                case "image/svg+xml" -> ".svg";
                case "video/mp4" -> ".mp4";
                case "video/quicktime" -> ".mov";
                case "video/webm" -> ".webm";
                default -> "";
            };
        }
        return "";
    }

    public enum MediaType {IMAGE, VIDEO}
}

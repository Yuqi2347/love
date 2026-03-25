package com.campus.love.common.service;

import com.campus.love.common.util.ImageCompressionUtil;
import com.campus.love.common.util.VideoTranscodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * 统一文件上传：按用户分子目录 {@code uploads/{userId}/}；图片在服务端压成 JPEG（长边≤1920）；
 * 视频在可用 ffmpeg 时转 H.264 MP4。
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${app.upload.path:${user.home}/campus-love/uploads/}")
    private String uploadPath;

    public static final String MIME_IMAGE_PREFIX = "image/";
    public static final String MIME_VIDEO_PREFIX = "video/";

    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg"};
    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".mov", ".webm", ".avi", ".mkv"};

    private static final int FEED_IMAGE_MAX_EDGE = 1920;
    private static final float FEED_JPEG_QUALITY = 0.82f;
    private static final int FEED_THUMB_MAX_EDGE = 520;
    private static final float FEED_THUMB_JPEG_QUALITY = 0.74f;

    public String uploadMedia(MultipartFile file, MediaType mediaType, long maxSizeBytes,
                             Long ownerUserId, String filenamePrefix, String defaultExt) throws IOException {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
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
            String limit = (maxSizeBytes / 1024 / 1024) + "MB";
            String hint = mediaType == MediaType.IMAGE
                    ? "图片大小不能超过" + limit
                    : "视频大小不能超过" + limit;
            throw new IllegalArgumentException(hint);
        }

        File dir = resolveUserUploadDir(ownerUserId);
        String uuid = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        if (mediaType == MediaType.IMAGE) {
            byte[] raw = file.getBytes();
            String ext = getFileExtension(file.getOriginalFilename(), contentType, mediaType);
            if (ext.isEmpty()) {
                ext = defaultExt;
            }
            if (!ImageCompressionUtil.shouldSkipCompression(contentType)
                    && ImageCompressionUtil.isLikelyRasterImage(contentType)) {
                byte[] jpg = ImageCompressionUtil.toCompressedJpeg(raw, FEED_IMAGE_MAX_EDGE, FEED_JPEG_QUALITY);
                if (jpg != null) {
                    raw = jpg;
                    ext = ".jpg";
                }
            }
            String filename = filenamePrefix + uuid + ext;
            Files.write(new File(dir, filename).toPath(), raw);
            writeFeedListThumbIfApplicable(dir, filenamePrefix, uuid, raw);
            return publicUrl(ownerUserId, filename);
        }

        String ext = getFileExtension(file.getOriginalFilename(), contentType, mediaType);
        if (ext.isEmpty()) {
            ext = defaultExt;
        }
        String base = filenamePrefix + uuid;
        File srcTmp = new File(dir, base + "_src" + ext);
        file.transferTo(srcTmp);
        File outMp4 = new File(dir, base + ".mp4");
        boolean trans = VideoTranscodeUtil.transcodeToMp4(srcTmp, outMp4);
        if (!trans) {
            String fallbackName = base + ext;
            File fallback = new File(dir, fallbackName);
            Files.move(srcTmp.toPath(), fallback.toPath(), StandardCopyOption.REPLACE_EXISTING);
            VideoTranscodeUtil.deleteQuietly(outMp4);
            log.info("Video stored without transcode: {}", fallbackName);
            return publicUrl(ownerUserId, fallbackName);
        }
        VideoTranscodeUtil.deleteQuietly(srcTmp);
        log.info("Video transcoded to MP4: {}", outMp4.getName());
        return publicUrl(ownerUserId, outMp4.getName());
    }

    /** 动态/聊天等图片：上传上限 25MB，落盘为压缩后 JPEG（或原 GIF/SVG 等） */
    public String uploadImage(MultipartFile file, Long ownerUserId, String filenamePrefix) throws IOException {
        return uploadMedia(file, MediaType.IMAGE, 25L * 1024 * 1024, ownerUserId, filenamePrefix, ".jpg");
    }

    public String uploadImage(MultipartFile file, Long ownerUserId, String filenamePrefix, long maxSizeBytes) throws IOException {
        return uploadMedia(file, MediaType.IMAGE, maxSizeBytes, ownerUserId, filenamePrefix, ".jpg");
    }

    /** 视频：上传上限 120MB，优先转码为压缩 MP4 */
    public String uploadVideo(MultipartFile file, Long ownerUserId, String filenamePrefix) throws IOException {
        return uploadMedia(file, MediaType.VIDEO, 120L * 1024 * 1024, ownerUserId, filenamePrefix, ".mp4");
    }

    private static String publicUrl(Long userId, String filename) {
        return "/uploads/" + userId + "/" + filename;
    }

    private void writeFeedListThumbIfApplicable(File dir, String filenamePrefix, String uuid, byte[] mainImageBytes) {
        if (!"feed_img_".equals(filenamePrefix) || mainImageBytes == null || mainImageBytes.length == 0) {
            return;
        }
        try {
            byte[] thumb = ImageCompressionUtil.toCompressedJpeg(
                    mainImageBytes, FEED_THUMB_MAX_EDGE, FEED_THUMB_JPEG_QUALITY);
            if (thumb != null) {
                String thumbName = "thumb_" + filenamePrefix + uuid + ".jpg";
                Files.write(new File(dir, thumbName).toPath(), thumb);
            }
        } catch (Exception e) {
            log.debug("Feed list thumb skipped: {}", e.getMessage());
        }
    }

    /** 根上传目录（各用户子目录的父目录） */
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

    private File resolveUserUploadDir(Long userId) {
        File base = getUploadDir();
        File dir = new File(base, String.valueOf(userId));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建用户上传目录: " + dir.getAbsolutePath());
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

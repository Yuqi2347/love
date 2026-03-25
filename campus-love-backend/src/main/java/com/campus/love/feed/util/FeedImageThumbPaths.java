package com.campus.love.feed.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 动态 feed 图列表缩略图：与 {@link com.campus.love.common.service.FileUploadService} 一致。
 * 新路径 {@code /uploads/{userId}/feed_img_* }；兼容旧路径 {@code /uploads/feed_img_* }（根目录平铺）。
 */
public final class FeedImageThumbPaths {

    private FeedImageThumbPaths() {}

    private static final String UPLOADS = "/uploads/";

    public static String buildImageThumbsCsv(String imagesCsv) {
        if (imagesCsv == null || imagesCsv.isBlank()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String raw : imagesCsv.split(",")) {
            String p = raw.trim();
            if (p.isEmpty()) {
                continue;
            }
            String thumb = thumbUrlOrNull(p);
            parts.add(thumb != null ? thumb : p);
        }
        return parts.isEmpty() ? null : String.join(",", parts);
    }

    /**
     * {@code feed_img_*} 位图：列表用同目录下 {@code thumb_} + 主图去扩展名 + {@code .jpg}；
     * 旧数据根目录平铺时缩略图为 {@code /uploads/thumb_*} .jpg。
     */
    public static String thumbUrlOrNull(String imagePath) {
        if (imagePath == null) {
            return null;
        }
        String p = imagePath.trim();
        if (!p.startsWith(UPLOADS)) {
            return null;
        }
        String rest = p.substring(UPLOADS.length());
        String filename;
        String dirPrefix;
        int slash = rest.indexOf('/');
        if (slash < 0) {
            dirPrefix = "";
            filename = rest;
        } else {
            String seg = rest.substring(0, slash);
            if (!seg.chars().allMatch(Character::isDigit)) {
                return null;
            }
            dirPrefix = seg + "/";
            filename = rest.substring(slash + 1);
        }
        if (!filename.startsWith("feed_img_")) {
            return null;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String ext = lower.substring(dot);
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")
                && !ext.equals(".webp") && !ext.equals(".bmp")) {
            return null;
        }
        String base = filename.substring(0, dot);
        return UPLOADS + dirPrefix + "thumb_" + base + ".jpg";
    }
}

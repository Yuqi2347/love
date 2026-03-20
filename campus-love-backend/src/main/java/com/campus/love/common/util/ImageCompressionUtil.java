package com.campus.love.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 将常见位图压缩为 JPEG（限制长边），减小存储与带宽；GIF/SVG 等保持原样。
 */
@Slf4j
public final class ImageCompressionUtil {

    private ImageCompressionUtil() {}

    public static boolean isLikelyRasterImage(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith("image/jpeg")
                || contentType.startsWith("image/png")
                || contentType.startsWith("image/webp")
                || contentType.startsWith("image/bmp");
    }

    public static boolean shouldSkipCompression(String contentType) {
        if (contentType == null) return true;
        return contentType.startsWith("image/gif")
                || contentType.startsWith("image/svg")
                || contentType.startsWith("image/x-icon");
    }

    /**
     * @param maxEdge 长边最大像素（头像可用 512，动态图 1920）
     * @return 压缩后的 JPEG 字节；失败或无需处理时返回 null，调用方保留原文件
     */
    public static byte[] toCompressedJpeg(byte[] input, int maxEdge, float quality) {
        if (input == null || input.length == 0) return null;
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return null;

            int w = src.getWidth();
            int h = src.getHeight();
            if (w <= 0 || h <= 0) return null;

            double scale = 1.0;
            if (w > maxEdge || h > maxEdge) {
                scale = Math.min((double) maxEdge / w, (double) maxEdge / h);
            }
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));

            BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, nw, nh);
            g2.drawImage(src, 0, 0, nw, nh, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(input.length, 2_000_000));
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                log.warn("No JPEG ImageWriter available");
                return null;
            }
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.5f, Math.min(0.95f, quality)));
            }
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
            writer.dispose();
            ios.close();
            byte[] out = baos.toByteArray();
            return out.length > 0 ? out : null;
        } catch (IOException e) {
            log.debug("Image compression skipped: {}", e.getMessage());
            return null;
        }
    }
}

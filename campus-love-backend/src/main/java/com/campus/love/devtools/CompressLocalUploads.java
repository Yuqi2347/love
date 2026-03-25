package com.campus.love.devtools;

import com.campus.love.common.util.ImageCompressionUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一次性处理上传根目录（含 {@code uploads/{userId}/} 子目录）：已有 JPEG 压到长边≤1920、质量 0.82（仅当新体积明显更小时覆盖）；
 * 为 {@code feed_img_*} 补列表缩略图 {@code thumb_feed_img_* .jpg}（主图为 JPG 或 PNG/WebP/BMP 均可）。
 * <p>
 * 用法：
 * <pre>
 *   cd campus-love-backend
 *   ./compress-uploads.sh
 *   # 或
 *   mvn -q compile exec:java -Dexec.mainClass=com.campus.love.devtools.CompressLocalUploads \
 *       -Dexec.args="/绝对路径/uploads"
 * </pre>
 * 未传参数时用环境变量 {@code APP_UPLOAD_PATH}，再退回 {@code ~/campus-love/uploads/}。
 * <p>
 * 不修改数据库；主图 PNG 等保持路径与扩展名不变，仅额外写入缩略图文件供探索列表使用。
 */
public final class CompressLocalUploads {

    private static final int MAX_EDGE = 1920;
    private static final float JPEG_Q = 0.82f;
    private static final int THUMB_EDGE = 520;
    private static final float THUMB_Q = 0.74f;
    private static final double MIN_SHRINK_RATIO = 0.97;

    private CompressLocalUploads() {}

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : System.getenv().getOrDefault("APP_UPLOAD_PATH",
                System.getProperty("user.home") + "/campus-love/uploads/");
        Path root = Paths.get(dir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.err.println("Not a directory: " + root);
            System.exit(1);
        }
        AtomicInteger jpegRewritten = new AtomicInteger();
        AtomicInteger jpegSkipped = new AtomicInteger();
        AtomicInteger thumbsCreated = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString();
                if (name.startsWith(".")) {
                    return FileVisitResult.CONTINUE;
                }
                String lower = name.toLowerCase(Locale.ROOT);

                if (name.startsWith("feed_img_") && !name.startsWith("thumb_")) {
                    if (lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
                        try {
                            writeFeedRasterThumbIfMissing(file, name, thumbsCreated);
                        } catch (IOException e) {
                            errors.incrementAndGet();
                            System.err.println("Thumb from raster: " + file + " — " + e.getMessage());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }

                if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
                    return FileVisitResult.CONTINUE;
                }
                if (name.startsWith("thumb_")) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    byte[] raw = Files.readAllBytes(file);
                    if (raw.length == 0) {
                        return FileVisitResult.CONTINUE;
                    }
                    byte[] compressed = ImageCompressionUtil.toCompressedJpeg(raw, MAX_EDGE, JPEG_Q);
                    byte[] finalBytes = raw;
                    if (compressed != null && compressed.length < raw.length * MIN_SHRINK_RATIO) {
                        Files.write(file, compressed);
                        jpegRewritten.incrementAndGet();
                        finalBytes = compressed;
                    } else {
                        jpegSkipped.incrementAndGet();
                    }
                    if (name.startsWith("feed_img_")) {
                        writeFeedJpegThumbIfMissing(file, name, finalBytes, thumbsCreated);
                    }
                } catch (IOException e) {
                    errors.incrementAndGet();
                    System.err.println("Error: " + file + " — " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.printf("CompressLocalUploads root=%s jpegRewritten=%d jpegSkippedOrNoGain=%d thumbsCreated=%d errors=%d%n",
                root, jpegRewritten.get(), jpegSkipped.get(), thumbsCreated.get(), errors.get());
    }

    private static void writeFeedRasterThumbIfMissing(Path file, String name, AtomicInteger thumbsCreated) throws IOException {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }
        String base = name.substring(0, dot);
        Path thumb = file.getParent().resolve("thumb_" + base + ".jpg");
        if (Files.exists(thumb)) {
            return;
        }
        byte[] raw = Files.readAllBytes(file);
        if (raw.length == 0) {
            return;
        }
        byte[] t = ImageCompressionUtil.toCompressedJpeg(raw, THUMB_EDGE, THUMB_Q);
        if (t != null) {
            Files.write(thumb, t);
            thumbsCreated.incrementAndGet();
        }
    }

    private static void writeFeedJpegThumbIfMissing(Path file, String name, byte[] imageBytes, AtomicInteger thumbsCreated)
            throws IOException {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }
        String base = name.substring(0, dot);
        Path thumb = file.getParent().resolve("thumb_" + base + ".jpg");
        if (Files.exists(thumb)) {
            return;
        }
        byte[] t = ImageCompressionUtil.toCompressedJpeg(imageBytes, THUMB_EDGE, THUMB_Q);
        if (t != null) {
            Files.write(thumb, t);
            thumbsCreated.incrementAndGet();
        }
    }
}

package com.campus.love.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 可选使用系统 ffmpeg 将视频压成 H.264 + AAC 的 MP4，降低体积。
 * 未安装 ffmpeg 或转码失败时返回 false，由调用方保留原文件。
 */
@Slf4j
public final class VideoTranscodeUtil {

    private VideoTranscodeUtil() {}

    public static boolean transcodeToMp4(File input, File output) {
        if (input == null || !input.isFile() || output == null) return false;
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(input.getAbsolutePath());
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-crf");
        cmd.add("28");
        cmd.add("-preset");
        cmd.add("fast");
        cmd.add("-vf");
        cmd.add("scale='min(1280,iw)':-2");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add("96k");
        cmd.add("-movflags");
        cmd.add("+faststart");
        cmd.add(output.getAbsolutePath());

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean done = p.waitFor(180, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                log.warn("ffmpeg timeout for {}", input.getName());
                return false;
            }
            if (p.exitValue() != 0) {
                log.warn("ffmpeg exit {} for {}", p.exitValue(), input.getName());
                return false;
            }
            return output.isFile() && output.length() > 0;
        } catch (IOException e) {
            log.debug("ffmpeg not available or failed to start: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 转码成功后删除源文件（源与目标为不同路径时） */
    public static void deleteQuietly(File f) {
        try {
            Files.deleteIfExists(f.toPath());
        } catch (IOException ignored) { }
    }
}

package com.campus.love.feed.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.feed.dto.FeedCommentRequest;
import com.campus.love.feed.dto.FeedPostRequest;
import com.campus.love.feed.dto.FeedPostResponse;
import com.campus.love.feed.service.FeedService;
import com.campus.love.user.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "朋友圈", description = "动态发布与互动")
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final ActivityService activityService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Operation(summary = "发布动态")
    @PostMapping
    public Result<FeedPostResponse> createPost(@Valid @RequestBody FeedPostRequest request) {
        return Result.success(feedService.createPost(request));
    }

    @Operation(summary = "发布发现模块动态")
    @PostMapping("/discovery")
    public Result<FeedPostResponse> createDiscoveryPost(@Valid @RequestBody FeedPostRequest request) {
        return Result.success(feedService.createDiscoveryPost(request));
    }

    @Operation(summary = "获取朋友圈动态流", description = "互相关注用户的动态")
    @GetMapping("/timeline")
    public Result<List<FeedPostResponse>> getTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getTimeline(page, size));
    }

    @Operation(summary = "获取用户动态列表")
    @GetMapping("/user/{userId}")
    public Result<List<FeedPostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserPosts(userId, page, size));
    }

    @Operation(summary = "获取用户的朋友圈帖子")
    @GetMapping("/user/{userId}/timeline")
    public Result<List<FeedPostResponse>> getUserTimelinePosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserTimelinePosts(userId, page, size));
    }

    @Operation(summary = "获取用户的发现模块帖子")
    @GetMapping("/user/{userId}/discovery")
    public Result<List<FeedPostResponse>> getUserDiscoveryPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserDiscoveryPosts(userId, page, size));
    }

    @Operation(summary = "点赞")
    @PostMapping("/like/{postId}")
    public Result<Void> like(@PathVariable Long postId) {
        feedService.likePost(postId);
        return Result.success();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/like/{postId}")
    public Result<Void> unlike(@PathVariable Long postId) {
        feedService.unlikePost(postId);
        return Result.success();
    }

    @Operation(summary = "评论")
    @PostMapping("/comment")
    public Result<Void> comment(@Valid @RequestBody FeedCommentRequest request) {
        feedService.addComment(request);
        return Result.success();
    }

    @Operation(summary = "删除动态")
    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        feedService.deletePost(postId);
        return Result.success();
    }

    @Operation(summary = "获取帖子详情", description = "单条帖子及完整评论列表（爬楼式）")
    @GetMapping("/{postId}")
    public Result<FeedPostResponse> getPostDetail(@PathVariable Long postId) {
        return Result.success(feedService.getPostDetail(postId));
    }

    @Operation(summary = "获取发现模块帖子列表", description = "按时间排序，展示管理员和高级用户发布的帖子")
    @GetMapping("/discovery")
    public Result<List<FeedPostResponse>> getDiscoveryPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getDiscoveryPosts(page, size));
    }

    @Operation(summary = "获取我的等级信息")
    @GetMapping("/level-info")
    public Result<ActivityService.UserLevelInfo> getLevelInfo() {
        return Result.success(activityService.getUserLevelInfo(null));
    }

    @Operation(summary = "我发布的帖子收到的新点赞/评论数量（用于导航红点）")
    @GetMapping("/activity/new-count")
    public Result<Integer> getNewFeedActivityCount() {
        return Result.success(feedService.getNewFeedActivityCount(CurrentUser.getId()));
    }

    @Operation(summary = "标记朋友圈活动已查看，消除红点")
    @PutMapping("/activity/mark-viewed")
    public Result<Void> markFeedActivityViewed() {
        feedService.markFeedActivityViewed(CurrentUser.getId());
        return Result.success();
    }

    @Operation(summary = "上传朋友圈图片")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error(400, "请选择文件");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error(400, "仅支持图片格式");
            }
            // 限制图片大小 10MB
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.error(400, "图片大小不能超过10MB");
            }

            String ext = getFileExtension(file.getOriginalFilename(), contentType);
            if (ext.isEmpty()) {
                ext = ".jpg"; // 默认扩展名
            }
            String filename = "feed_img_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            File uploadDir = getUploadDir();
            File dest = new File(uploadDir, filename);
            file.transferTo(dest);

            return Result.success("上传成功", "/uploads/" + filename);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "上传朋友圈视频")
    @PostMapping("/upload/video")
    public Result<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error(400, "请选择文件");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return Result.error(400, "仅支持视频格式");
            }
            // 限制视频大小 100MB
            if (file.getSize() > 100 * 1024 * 1024) {
                return Result.error(400, "视频大小不能超过100MB");
            }

            String ext = getFileExtension(file.getOriginalFilename(), contentType);
            if (ext.isEmpty()) {
                ext = ".mp4"; // 默认扩展名
            }
            String filename = "feed_video_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            File uploadDir = getUploadDir();
            File dest = new File(uploadDir, filename);
            file.transferTo(dest);

            return Result.success("上传成功", "/uploads/" + filename);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传目录的绝对路径，确保目录存在
     */
    private File getUploadDir() {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadPath);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private String getFileExtension(String filename, String contentType) {
        // 优先从文件名获取扩展名
        if (filename != null && filename.contains(".")) {
            String lower = filename.toLowerCase();
            int lastDot = lower.lastIndexOf('.');
            String ext = lower.substring(lastDot);
            // 支持的图片格式
            if (ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") ||
                ext.equals(".gif") || ext.equals(".webp") || ext.equals(".bmp") ||
                ext.equals(".svg")) {
                return ext;
            }
            // 支持的视频格式
            if (ext.equals(".mp4") || ext.equals(".mov") || ext.equals(".webm") ||
                ext.equals(".avi") || ext.equals(".mkv")) {
                return ext;
            }
        }
        // 从 contentType 推断扩展名
        if (contentType != null) {
            if (contentType.equals("image/jpeg")) return ".jpg";
            if (contentType.equals("image/png")) return ".png";
            if (contentType.equals("image/gif")) return ".gif";
            if (contentType.equals("image/webp")) return ".webp";
            if (contentType.equals("image/svg+xml")) return ".svg";
            if (contentType.equals("video/mp4")) return ".mp4";
            if (contentType.equals("video/quicktime")) return ".mov";
            if (contentType.equals("video/webm")) return ".webm";
        }
        return "";
    }
}
